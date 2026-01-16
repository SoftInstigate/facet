package org.facet.html.internal;

import java.util.Map;

import org.facet.templates.TemplateContextBuilder;
import org.facet.templates.TemplateProcessingException;
import org.facet.templates.TemplateProcessor;
import org.restheart.exchange.Request;
import org.restheart.exchange.ServiceRequest;
import org.restheart.exchange.ServiceResponse;
import org.restheart.utils.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;

/**
 * Utility methods for handling HTML responses.
 */
public class HtmlResponseHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlResponseHelper.class);

    /**
     * Checks if the request should be treated as HTML-capable.
     *
     * A request is considered HTML-capable when either:
     * - It explicitly advertises {@code Accept: text/html}
     * - It is an HTMX request (i.e., {@code HX-Request: true} AND {@code Accept: *&#47;*})
     *
     * This allows proper server-side content negotiation even when HTMX sends
     * a generic {@code Accept: *&#47;*} header.
     *
     * @param headers the request headers
     * @return true if the request should be processed as HTML, false otherwise
     */
    public static boolean acceptsHtml(final HeaderMap headers) {
        // 1) Standard browser navigation: Accept includes text/html
        final boolean acceptHtml = headers.contains(Headers.ACCEPT)
                && headers.get(Headers.ACCEPT).stream().anyMatch(v -> v.contains("text/html"));

        if (acceptHtml) {
            return true;
        }

        // 2) HTMX progressive enhancement: HX-Request: true AND Accept: */*
        // Undertow's HeaderMap is case-insensitive; check value equalsIgnoreCase("true")
        return HtmxRequestDetector.isHtmxRequest(headers);
    }

    /**
     * Disables caching headers on the response (useful for development mode).
     * This is a convenience method when no caching is needed.
     *
     * @param response the service response
     */
    public static void disableCaching(final ServiceResponse<?> response) {
        disableCachingForDevelopment(response);
    }

    /**
     * Sets caching headers on the response based on the development mode.
     * Also checks If-None-Match header and returns 304 if ETag matches.
     *
     * @param request the service request
     * @param response the service response
     * @param hashCode the hash code of the content for ETag
     * @param enableResponseCaching true to enable caching, false to disable (development mode)
     * @param maxAge the Cache-Control max-age value in seconds
     * @return true if 304 was sent, false if full response should be sent
     */
    public static boolean setCachingHeaders(final ServiceRequest<?> request, final ServiceResponse<?> response,
            final int hashCode, final boolean enableResponseCaching, final int maxAge) {
        if (enableResponseCaching) {
            return applyCachingHeaders(request, response, hashCode, maxAge);
        } else {
            disableCachingForDevelopment(response);
            return false;
        }
    }

    /**
     * Renders an HTML error page for the given request/response.
     *
     * <p>
     * This method sets a custom sender on the response that:
     * <ol>
     * <li>Sets Content-Type to text/html</li>
     * <li>Builds error template context with status code, message, path, username</li>
     * <li>Renders error.html template</li>
     * <li>Falls back to simple HTML if template processing fails</li>
     * </ol>
     *
     * @param request the service request
     * @param response the service response
     * @param templateProcessor the template processor for rendering error.html
     */
    public static void renderErrorPage(
            final ServiceRequest<?> request,
            final ServiceResponse<?> response,
            final TemplateProcessor templateProcessor) {

        LOGGER.debug("Rendering error page for request: {}", request.getPath());

        response.setCustomSender(() -> {
            response.setContentTypeAsHtml();
            final var exchange = response.getExchange();
            try {
                // Build error template context
                final var req = Request.of(exchange);
                final String username = req.getAuthenticatedAccount() != null
                    ? req.getAuthenticatedAccount().getPrincipal().getName()
                    : null;

                final Map<String, Object> context = new TemplateContextBuilder(
                        templateProcessor.getGlobalTemplateContext())
                                .with("statusCode", response.getStatusCode())
                                .with("statusMessage", HttpStatus.getStatusText(response.getStatusCode()))
                                .with("path", request.getPath())
                                .with("username", username)
                                .build();

                // Render error template
                final String html = templateProcessor.process("error", context);
                exchange.getResponseSender().send(html);

            } catch (final TemplateProcessingException e) {
                LOGGER.error("Error rendering error template for path: {}", request.getPath(), e);
                // Fallback HTML if template fails
                final String fallbackHtml = createFallbackErrorHtml(response.getStatusCode());
                exchange.getResponseSender().send(fallbackHtml);
            }
        });
    }

    private static void disableCachingForDevelopment(final ServiceResponse<?> response) {
        // Completely disables caching for development
        response.setHeader(Headers.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        response.setHeader(Headers.PRAGMA, "no-cache"); // Add for HTTP/1.0 compatibility
        response.setHeader(Headers.EXPIRES, "0"); // Add for older browsers
    }

    private static boolean applyCachingHeaders(
            final ServiceRequest<?> request,
            final ServiceResponse<?> response,
            final int contentHashCode,
            final int maxAge) {
        // Generate ETag from content hash
        final String etag = '"' + Integer.toHexString(contentHashCode) + '"';

        // Check If-None-Match header for ETag validation
        final String ifNoneMatch = request.getHeader("If-None-Match");
        LOGGER.debug("ETag validation - Request: {}, Generated: {}, Match: {}",
                ifNoneMatch, etag, (ifNoneMatch != null && ifNoneMatch.equals(etag)));

        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
            // ETag matches - return true to signal 304
            // Status code will be set by caller
            LOGGER.debug("ETag matches - will send 304 Not Modified for path: {}", request.getPath());
            response.setHeader(Headers.ETAG, etag);
            response.setHeader(Headers.CACHE_CONTROL, "private, max-age=" + maxAge + ", must-revalidate");
            return true; // Signal 304, don't send body
        }

        // ETag doesn't match or not present - send full response with caching headers
        // Set a reasonable default cache policy for rendered HTML
        // max-age balances performance with freshness - ETag validation happens after max-age seconds
        response.setHeader(Headers.CACHE_CONTROL, "private, max-age=" + maxAge + ", must-revalidate");

        // Ensure Vary contains "Accept-Encoding" but do not clobber any existing Vary values.
        // If the response already has a Vary header, append Accept-Encoding when missing.
        try {
            final var respHeaders = response.getExchange().getResponseHeaders();
            final var existingVary = respHeaders.get(Headers.VARY);
            if (existingVary == null || existingVary.isEmpty()) {
                respHeaders.put(Headers.VARY, "Accept-Encoding");
            } else {
                final String combined = String.join(", ", existingVary);
                if (!combined.toLowerCase().contains("accept-encoding")) {
                    respHeaders.put(Headers.VARY, combined + ", Accept-Encoding");
                }
            }
        } catch (final Exception e) {
            // Be conservative: if anything goes wrong querying existing headers, set the value on the response.
            LOGGER.warn("Failed to check existing Vary header: {}", e.getMessage());
            response.setHeader(Headers.VARY, "Accept-Encoding");
        }

        // Strong ETag based on provided content hash. Keep quoting per RFC.
        response.setHeader(Headers.ETAG, etag);
        return false; // Send full response body
    }

    /**
     * Creates a simple HTML error page when template processing fails.
     *
     * <p>
     * This is a fallback that ensures users always see something meaningful
     * even if the template system is broken.
     *
     * @param statusCode the HTTP status code
     * @return the fallback HTML content
     */
    private static String createFallbackErrorHtml(final int statusCode) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Error %d</title>
                    <style>
                        body { font-family: sans-serif; margin: 40px; text-align: center; }
                        h1 { color: #d32f2f; }
                    </style>
                </head>
                <body>
                    <h1>Error %d</h1>
                    <p>%s</p>
                    <p>An error occurred while processing your request.</p>
                </body>
                </html>
                """.formatted(statusCode, statusCode, HttpStatus.getStatusText(statusCode));
    }

    private HtmlResponseHelper() {
        // Utility class, no instantiation
    }
}
