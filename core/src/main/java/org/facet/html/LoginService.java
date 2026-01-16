package org.facet.html;

import java.util.HashMap;
import java.util.Map;

import org.facet.html.internal.HtmlResponseHelper;
import org.facet.templates.TemplateProcessor;
import org.jspecify.annotations.NonNull;
import org.restheart.exchange.JsonRequest;
import org.restheart.exchange.JsonResponse;
import org.restheart.plugins.Inject;
import org.restheart.plugins.Interceptor;
import org.restheart.plugins.JsonService;
import org.restheart.plugins.OnInit;
import org.restheart.plugins.PluginRecord;
import org.restheart.plugins.PluginsRegistry;
import org.restheart.plugins.RegisterPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.undertow.util.Headers;

/**
 * Generic login service for RESTHeart cookie authentication.
 * 
 * <p>
 * Returns JSON data for GET /login; HtmlResponseInterceptor renders templates/login/index.html.
 * Any client can submit credentials to /roles/{username}?set-auth-cookie (RESTHeart core).
 * 
 * <h3>Authentication Flow</h3>
 * <ol>
 * <li>GET /login → returns JSON model → HtmlResponseInterceptor renders template</li>
 * <li>Client submits credentials → calls GET /roles/{username}?set-auth-cookie with Basic Auth</li>
 * <li>RESTHeart's authCookieSetter validates credentials and sets rh_auth HttpOnly cookie</li>
 * <li>Logout handled by RESTHeart's authCookieRemover (POST /logout)</li>
 * </ol>
 * 
 * <h3>Configuration</h3>
 *
 * <pre>
 * /login-service:
 *   enabled: true
 *   uri: /login                    # Mandatory: mount point
 *   redirect-param: redirect       # Query param for post-login redirect (default: "redirect")
 *   default-redirect: /api         # Default redirect when no redirect param provided (default: "/")
 *   roles-endpoint: /roles         # Where clients send credentials (default: "/roles")
 * </pre>
 * 
 * <h3>Required Plugins</h3>
 * <ul>
 * <li>pebble-template-processor - Template engine</li>
 * <li>html-response-interceptor - Transforms to HTML</li>
 * <li>authCookieSetter, authCookieHandler, authCookieRemover - RESTHeart cookie auth</li>
 * </ul>
 */
@RegisterPlugin(
    name = "login-service",
    description = "Generic login service for cookie-based authentication",
    enabledByDefault = false,
    secure = false)
public class LoginService implements JsonService {

    private static final String ERROR = "error";

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginService.class);

    @Inject("config")
    private Map<String, Object> config;

    @Inject("registry")
    private PluginsRegistry registry;

    @Inject("pebble-template-processor")
    private TemplateProcessor templateProcessor;

    private String loginUri;
    private String redirectParam;
    private String rolesEndpoint;
    private String defaultRedirect;

    @OnInit
    public void onInit() {
        if (config == null || !config.containsKey("uri")) {
            throw new IllegalStateException("login-service requires mandatory 'uri' configuration");
        }

        loginUri = config.get("uri").toString();
        redirectParam = config.getOrDefault("redirect-param", "redirect").toString();
        rolesEndpoint = config.getOrDefault("roles-endpoint", "/roles").toString();
        defaultRedirect = config.getOrDefault("default-redirect", "/").toString();

        // Add login context variables to global template context
        if (templateProcessor != null) {
            templateProcessor.addToGlobalTemplateContext("loginUri", loginUri);
            templateProcessor.addToGlobalTemplateContext("rolesEndpoint", rolesEndpoint);
            LOGGER.info("LoginService initialized at {} (roles endpoint: {})",
                    loginUri, rolesEndpoint);
        } else {
            LOGGER.warn("LoginService initialized at {} (template processor not available)", loginUri);
        }

        // Register HtmlAuthRedirectInterceptor with our login URI
        registerAuthRedirectInterceptor();
    }

    /**
     * Handles /login requests
     * 
     * @param request the request
     * @param response the response
     */
    @Override
    public void handle(final @NonNull JsonRequest request, final JsonResponse response) throws Exception {
        switch (request.getMethod()) {
            case GET -> handleGetLogin(request, response);
            case OPTIONS -> handleOptions(request);
            default -> {
                response.setStatusCode(405); // Method Not Allowed
                response.getExchange().getResponseHeaders().put(Headers.ALLOW, "GET, OPTIONS");
            }
        }
    }

    /**
     * GET /login - returns JSON model for login form template
     */
    private void handleGetLogin(final JsonRequest request, final JsonResponse response) {
        final var qp = request.getExchange().getQueryParameters();
        final var dest = qp.get(redirectParam) != null && !qp.get(redirectParam).isEmpty()
            ? qp.get(redirectParam).getFirst()
            : defaultRedirect;

        final String error = qp.get(ERROR) != null && !qp.get(ERROR).isEmpty()
            ? qp.get(ERROR).getFirst()
            : null;

        final JsonObject model = new JsonObject();
        model.addProperty("redirect", dest);
        model.addProperty("loginUri", loginUri);
        model.addProperty("rolesEndpoint", rolesEndpoint);
        if (error != null) {
            model.addProperty(ERROR, error);
        }

        HtmlResponseHelper.disableCaching(response);

        response.setStatusCode(200);
        response.setContent(model);
    }

    /**
     * Registers HtmlAuthRedirectInterceptor dynamically with login URI.
     * Ensures unauthenticated browser requests are redirected to login page.
     */
    private void registerAuthRedirectInterceptor() {
        // Check if interceptor is already registered (via configuration or BrowserService)
        if (registry.getInterceptors().stream()
                .anyMatch(i -> i.getInstance() instanceof HtmlAuthRedirectInterceptor)) {
            LOGGER.warn("HtmlAuthRedirectInterceptor already registered, skipping");
            return;
        }

        try {
            final var interceptorConfig = new HashMap<String, Object>();
            interceptorConfig.put("login-uri", loginUri);

            final var interceptor = new HtmlAuthRedirectInterceptor(interceptorConfig);
            final PluginRecord<Interceptor<?, ?>> pr = new PluginRecord<>(
                    "html-auth-redirect-interceptor",
                    "Redirects browser requests to login page on authentication failure",
                    false, // secure
                    true, // enabled
                    HtmlAuthRedirectInterceptor.class.getName(),
                    interceptor,
                    interceptorConfig);

            registry.addInterceptor(pr);
            LOGGER.info("Dynamically registered HtmlAuthRedirectInterceptor with login-uri: {}", loginUri);
        } catch (final Exception e) {
            LOGGER.error("Failed to register HtmlAuthRedirectInterceptor", e);
        }
    }
}
