package org.facet.html;

import static org.facet.html.internal.HtmlResponseHelper.acceptsHtml;
import static org.facet.html.internal.HtmlResponseHelper.renderErrorPage;
import static org.facet.html.internal.HtmlResponseHelper.setCachingHeaders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.facet.html.handlers.HtmlResponseHandler;
import org.facet.html.handlers.JsonHtmlResponseHandler;
import org.facet.html.handlers.MongoHtmlResponseHandler;
import org.facet.html.internal.HtmxRequestDetector;
import org.facet.templates.PathBasedTemplateResolver;
import org.facet.templates.TemplateProcessingException;
import org.facet.templates.TemplateProcessor;
import org.facet.templates.TemplateResolver;
import org.jspecify.annotations.NonNull;
import org.restheart.exchange.MongoRequest;
import org.restheart.exchange.ServiceRequest;
import org.restheart.exchange.ServiceResponse;
import org.restheart.mongodb.utils.MongoMountResolver.ResolvedContext;
import org.restheart.plugins.Inject;
import org.restheart.plugins.InterceptPoint;
import org.restheart.plugins.OnInit;
import org.restheart.plugins.RegisterPlugin;
import org.restheart.plugins.WildcardInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoClient;

/**
 * Wildcard interceptor that transforms responses to HTML for browser clients.
 *
 * <p>
 * This interceptor handles responses from <strong>any service</strong> that reaches the RESPONSE
 * intercept point, transforming both successful (2xx) and error (4xx/5xx) responses to HTML:
 * <ul>
 * <li><strong>Success (2xx):</strong> Uses path-based templates via SSR framework</li>
 * <li><strong>Errors (4xx/5xx):</strong> Renders error.html template with fallback</li>
 * </ul>
 *
 * <p>
 * This interceptor uses the Strategy pattern to delegate response processing to
 * specialized handlers based on response type:
 * <ul>
 * <li>{@link MongoHtmlResponseHandler} - MongoDB BSON responses with pagination</li>
 * <li>{@link JsonHtmlResponseHandler} - Generic JSON responses (fallback)</li>
 * </ul>
 *
 * <p>
 * <strong>Activation criteria:</strong>
 * <ul>
 * <li>Request is HTML-capable: {@code Accept: text/html} or {@code HX-Request: true}</li>
 * <li>For success (2xx): Template exists for the request path</li>
 * <li>For errors (4xx/5xx): Always renders error.html template</li>
 * <li>Intercepts at RESPONSE phase with priority 5</li>
 * </ul>
 *
 * <p>
 * <strong>Division of labor with {@link HtmlErrorResponseInterceptor}:</strong>
 * <ul>
 * <li>This interceptor: Handles responses that reach RESPONSE phase (most common case)</li>
 * <li>HtmlErrorResponseInterceptor: Catches early errors that terminate during REQUEST_AFTER_AUTH</li>
 * </ul>
 *
 * <h3>Configuration</h3>
 *
 * <pre>
 * /html-response-interceptor:
 *   enabled: true
 *   response-caching: false  # Optional: disables ETag caching for development (default: false)
 *   max-age: 5               # Optional: Cache-Control max-age in seconds (default: 5)
 * </pre>
 *
 * <p>
 * <strong>Dependencies:</strong>
 * <ul>
 * <li>pebble-template-processor - Template engine</li>
 * <li>mclient - MongoDB client (for MongoHtmlResponseHandler)</li>
 * <li>rh-config - RESTHeart configuration (for mount resolution)</li>
 * </ul>
 *
 * @see HtmlResponseHandler
 * @see MongoHtmlResponseHandler
 * @see JsonHtmlResponseHandler
 * @see HtmlErrorResponseInterceptor
 */
@RegisterPlugin(
    name = "html-response-interceptor",
    description = "Generic interceptor that transforms responses (2xx and 4xx/5xx) to HTML using templates",
    interceptPoint = InterceptPoint.RESPONSE,
    priority = 5, // Executed before namespacesResponseFlattener, to allow rendering full json
    enabledByDefault = false)
public class HtmlResponseInterceptor implements WildcardInterceptor {

    private static final String HX_TARGET = "hxTarget";

    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlResponseInterceptor.class);

    // Determines if status code indicates access denied (401/403)
    private static boolean isAccessDenied(final int statusCode) {
        return statusCode == 401 || statusCode == 403;
    }

    // Retrieves status code, treating unset (-1) as 200
    private static int getStatusCode(final ServiceResponse<?> response) {
        return response.getStatusCode() < 0
            ? 200
            : response.getStatusCode();
    }

    // Determines if the response status code indicates success (2xx)
    private static boolean isSuccessfulResponse(final int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    // Sends full HTML response
    private static void sendFullHtmlResponse(final ServiceResponse<?> response, final String html) {
        response.setCustomSender(() -> {
            response.setContentTypeAsHtml();
            response.getExchange().getResponseSender().send(html);
        });
    }

    // Sends "304 Not Modified" response
    private static void sendNotModifiedResponse(final ServiceResponse<?> response) {
        response.setStatusCode(304);
        response.setCustomSender(() -> response.getExchange().endExchange());
    }

    // Reports missing fragment template for HTMX requests and sets 500 response.
    private static void reportMissingFragmentTemplate(
            final ServiceRequest<?> request,
            final ServiceResponse<?> response,
            final String hxTarget) {

        LOGGER.error("HTMX fragment template not found for path '{}' and target '{}'",
                request.getPath(), hxTarget);
        response.setStatusCode(500);
        response.setContentType("text/plain");
        response.setCustomSender(() -> response.getExchange().getResponseSender()
                .send("HTMX fragment template not found: " + hxTarget));
    }

    // Retrieves HX-Target from request, stripping leading '#' if present.
    private static String getHxTarget(final ServiceRequest<?> request) {
        final String rawTarget = HtmxRequestDetector.getHxTarget(request);
        return rawTarget != null && rawTarget.startsWith("#")
            ? rawTarget.substring(1)
            : rawTarget;
    }

    // Retrieves max-age from configuration, defaulting to 5 seconds.
    private static int getMaxAgeFromConfig(final Map<String, Object> config) {
        return config != null && config.containsKey("max-age")
            ? Integer.parseInt(config.get("max-age").toString())
            : 5;
    }

    // Determines if response caching is enabled from configuration.
    private static boolean isResponseCachingEnabled(final Map<String, Object> config) {
        return config != null
                && Boolean.parseBoolean(config.getOrDefault("response-caching", "true").toString());
    }

    @Inject("config")
    private Map<String, Object> pluginConfig;

    @Inject("pebble-template-processor")
    private TemplateProcessor templateProcessor;

    @Inject("mclient")
    private MongoClient mongoClient;

    private final List<HtmlResponseHandler> handlers = new ArrayList<>();

    private TemplateResolver templateResolver;

    private boolean isResponseCachingEnabled;

    private int maxAge;

    /**
     * Initializes the interceptor and registers response handlers.
     */
    @OnInit
    public void onInit() {
        this.isResponseCachingEnabled = isResponseCachingEnabled(this.pluginConfig);

        this.maxAge = getMaxAgeFromConfig(this.pluginConfig);

        // Create template resolver
        this.templateResolver = new PathBasedTemplateResolver();

        // Register handlers in priority order (first match wins)
        this.handlers.add(new MongoHtmlResponseHandler(mongoClient, templateProcessor));
        this.handlers.add(new JsonHtmlResponseHandler(templateProcessor)); // Fallback

        LOGGER.info("HTML Response Interceptor initialized with {} handlers", handlers.size());
        LOGGER.debug("Configuration: {}", pluginConfig);
    }

    /**
     * Determines if this interceptor should handle the request/response.
     *
     * @param request the service request
     * @param response the service response
     * @return true if response should be transformed to HTML
     */
    @Override
    public boolean resolve(final @NonNull ServiceRequest<?> request, final ServiceResponse<?> response) {
        // Only intercept when request accepts HTML
        if (!acceptsHtml(request.getHeaders())) {
            return false;
        }

        // Some services (e.g., built-in ping) may not set status code on the ServiceResponse
        // at the time interceptors run and return -1. Treat unset status as 200 for template
        // resolution so SSR can still render responses produced as raw bytes.
        final int statusCode = getStatusCode(response);

        // Never intercept 401/403 - allow authentication/authorization challenges to pass through
        if (isAccessDenied(statusCode)) {
            return false;
        }

        // For error responses (4xx/5xx except 401/403), render error.html
        if (statusCode >= 400) {
            return true;
        }

        if (request instanceof final MongoRequest mongoRequest) {
            final ResolvedContext ctx = mongoRequest.getResolvedContext();

            // First: Check if path has invalid extra segments (applies to ALL responses)
            // This must be checked before status code logic because we want to catch
            // invalid paths regardless of whether they're cached (304) or fresh (200)
            if (ctx.hasExtraPathSegments()) {
                LOGGER.debug("Detected path with extra segments, rendering as 404: {}", request.getPath());
                return true; // Render error page as 404
            }
        }

        // For success responses, check template existence (only if a resolver is configured)
        if (isSuccessfulResponse(statusCode)) {
            // Check for template using request type for action-aware resolution
            final var templateName = resolveTemplateWithType(request);
            if (templateName.isPresent()) {
                LOGGER.debug("HtmlResponseInterceptor.resolve - template resolved: {}", templateName.get());
                return true;
            }
        }

        // Otherwise, do not intercept
        return false;
    }

    /**
     * Transforms response to HTML using appropriate handler.
     *
     * @param request the service request
     * @param response the service response
     */
    @Override
    public void handle(final ServiceRequest<?> request, final @NonNull ServiceResponse<?> response) {
        final int statusCode = response.getStatusCode();

        if (request instanceof final MongoRequest mongoRequest) {
            final var resolvedContext = mongoRequest.getResolvedContext();

            // Check for paths with invalid extra segments and treat as 404
            if (isSuccessfulResponse(statusCode) && resolvedContext.hasExtraPathSegments()) {
                LOGGER.debug("Handling path with extra segments as 404: {}", request.getPath());
                response.setStatusCode(404);
                handleErrorResponse(request, response);
                return;
            }
        }

        // Handle error responses (4xx/5xx) differently
        if (statusCode >= 400) {
            handleErrorResponse(request, response);
        } else {
            handleSuccessResponse(request, response);
        }
    }

    /**
     * Handles successful (2xx) responses using the SSR framework.
     *
     * For HTMX requests, attempts to return fragment templates instead of full pages.
     *
     * @param request the service request
     * @param response the service response
     */
    private void handleSuccessResponse(final ServiceRequest<?> request, final ServiceResponse<?> response) {
        LOGGER.debug("Transforming success response to HTML for path: {}", request.getPath());

        try {
            // Find appropriate handler (first match wins)
            final var handler = getHandler(request, response);

            if (handler.isEmpty()) {
                LOGGER.warn("No handler found for request: {}", request.getPath());
                return;
            }

            // Build template context using handler
            final var templateContext = handler.get().buildContext(request, response);

            // Detect HTMX request
            final boolean isHtmxRequest = HtmxRequestDetector.isHtmxRequest(request);

            // Add HTMX context for templates
            templateContext.put("isHtmxRequest", isHtmxRequest);

            if (isHtmxRequest) {
                // For HTMX requests with HX-Target, use fragment template (strict mode - must exist)

                LOGGER.debug("HTMX request detected for path: {}", request.getPath());

                // Get HX-Target (without leading '#')
                final String hxTarget = getHxTarget(request);

                if (hxTarget != null && !hxTarget.isBlank()) {
                    // Add HX target to template context
                    templateContext.put(HX_TARGET, hxTarget);

                    // Resolve fragment template by hxTarget
                    final var templateName = this.templateResolver.resolveFragment(
                            this.templateProcessor,
                            request.getPath(),
                            hxTarget);

                    if (templateName.isEmpty()) {
                        // Fragment not found - this is an error in strict mode
                        reportMissingFragmentTemplate(request, response, hxTarget);
                        return;
                    }
                    LOGGER.debug("HTMX request for target '{}', using fragment template: {}", hxTarget,
                            templateName.get());
                    checkAndSendResponse(request, response, templateContext, templateName.get());
                    return;
                }
            }

            final var templateName = resolveTemplateWithType(request);

            if (templateName.isEmpty()) {
                LOGGER.error("Template resolution failed for path: {}, cannot render HTML", request.getPath());
                return;
            }

            checkAndSendResponse(request, response, templateContext, templateName.get());

        } catch (final TemplateProcessingException e) {
            LOGGER.error("Error processing template for path: {}", request.getPath(), e);
            // Don't modify response - let original response go through
        } catch (final Exception e) {
            LOGGER.error("Unexpected error transforming response to HTML for path: {}", request.getPath(), e);
            // Don't modify response - let original response go through
        }
    }

    /**
     * Handles error (4xx/5xx) responses by delegating to shared error renderer.
     *
     * @param request the service request
     * @param response the service response
     */
    private void handleErrorResponse(final ServiceRequest<?> request, final ServiceResponse<?> response) {
        renderErrorPage(request, response, this.templateProcessor);
    }

    // Finds the first handler that can process the given request/response.
    private Optional<HtmlResponseHandler> getHandler(
            final ServiceRequest<?> request,
            final ServiceResponse<?> response) {

        for (final HtmlResponseHandler h : handlers) {
            if (h.canHandle(request, response)) {
                final var handler = Optional.of(h);
                LOGGER.debug("Using handler: {}", h.getClass().getSimpleName());
                return handler;
            }
        }
        return Optional.empty();
    }

    /**
     * Resolves template using action-aware resolution when request type is available.
     * Falls back to path-only resolution for non-MongoDB requests.
     *
     * @param request the service request
     * @return the resolved template name if found
     */
    private Optional<String> resolveTemplateWithType(final ServiceRequest<?> request) {
        if (request instanceof MongoRequest mongoRequest) {
            // Build template resolution path using MongoDB context
            // This ensures templates are resolved correctly regardless of mongo-mount configuration
            final String templatePath = buildTemplateResolutionPath(mongoRequest);

            // Use action-aware resolution for MongoDB requests
            return templateResolver.resolve(templateProcessor, templatePath, mongoRequest.getType());
        } else {
            // Fallback to path-only resolution for other request types
            return templateResolver.resolve(templateProcessor, request.getPath());
        }
    }

    /**
     * Builds the template resolution path using MongoDB context.
     * Uses the mongoResourcePath from ResolvedContext which provides the canonical
     * MongoDB path regardless of how MongoDB is mounted.
     *
     * Examples:
     * - MongoDB at root (/): /docs/special-doc → /testdb/docs/special-doc
     * - MongoDB at /api: /api/testdb/docs/special-doc → /testdb/docs/special-doc
     *
     * @param mongoRequest the MongoDB request
     * @return the normalized path for template resolution
     */
    private String buildTemplateResolutionPath(final MongoRequest mongoRequest) {
        final var resolvedContext = mongoRequest.getResolvedContext();

        // Use mongoResourcePath from ResolvedContext (fixed in MongoMountResolverImpl)
        // This provides the canonical path: /database/collection/documentId
        if (resolvedContext != null) {
            final String mongoResourcePath = resolvedContext.mongoResourcePath();
            if (mongoResourcePath != null && !mongoResourcePath.isEmpty()) {
                LOGGER.debug("Using mongoResourcePath for template resolution: '{}'", mongoResourcePath);
                return mongoResourcePath;
            }
        }

        // Fallback (should not happen in normal operation)
        LOGGER.warn("ResolvedContext or mongoResourcePath is null, falling back to request path");
        return mongoRequest.getPath();
    }

    // Checks ETag and sends HTML response, handling "304 Not Modified" if applicable.
    private void checkAndSendResponse(
            final ServiceRequest<?> request,
            final ServiceResponse<?> response,
            final Map<String, Object> templateContext,
            final String templateName) throws TemplateProcessingException {

        LOGGER.debug("Using template '{}' for path: {}", templateName, request.getPath());

        // Render template
        final String html = this.templateProcessor.process(templateName, templateContext);

        // Check ETag before setting up custom sender
        final boolean is304 = setCachingHeaders(
                request,
                response,
                html.hashCode(),
                this.isResponseCachingEnabled,
                this.maxAge);

        if (is304) {
            sendNotModifiedResponse(response);
        } else {
            sendFullHtmlResponse(response, html);
        }
    }

}
