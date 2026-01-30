package org.facet.html;

import static org.facet.html.internal.HtmlResponseHelper.acceptsHtml;
import static org.facet.html.internal.HtmlResponseHelper.renderErrorPage;

import org.facet.templates.TemplateProcessor;
import org.restheart.exchange.ServiceRequest;
import org.restheart.exchange.ServiceResponse;
import org.restheart.plugins.Inject;
import org.restheart.plugins.InterceptPoint;
import org.restheart.plugins.RegisterPlugin;
import org.restheart.plugins.WildcardInterceptor;

/**
 * Wildcard interceptor that renders HTML error pages for early-phase errors.
 *
 * <p>This interceptor catches errors that occur during the REQUEST_AFTER_AUTH phase, before the
 * exchange reaches the RESPONSE intercept point. It handles errors from <strong>any service</strong>
 * that terminates the request early, including:
 * <ul>
 *   <li>MongoDB database not found (404)</li>
 *   <li>MongoDB collection not found (404)</li>
 *   <li>Other service errors that set response and terminate during REQUEST_AFTER_AUTH</li>
 * </ul>
 *
 * <p>Works in conjunction with {@link HtmlResponseInterceptor} which handles responses that
 * reach the RESPONSE phase (e.g., successful responses, document-by-ID errors).
 *
 * <p><strong>Why this is needed:</strong> Some services (notably MongoDB) detect errors during
 * REQUEST_AFTER_AUTH and set a custom response sender, terminating the exchange before it reaches
 * the RESPONSE intercept point. This interceptor catches those early errors for browser clients.
 *
 * <p><strong>Activation criteria:</strong>
 * <ul>
 *   <li>Request is HTML-capable: {@code Accept: text/html} or {@code HX-Request: true}</li>
 *   <li>Response is in error state (4xx/5xx status code)</li>
 *   <li>Intercepts at REQUEST_AFTER_AUTH with maximum priority (runs last)</li>
 * </ul>
 *
 * @see HtmlResponseInterceptor
 */
@RegisterPlugin(
    name = "html-error-response-interceptor",
    description = "Renders HTML error pages for early-phase errors (any service, e.g., MongoDB db/collection not found)",
    interceptPoint = InterceptPoint.REQUEST_AFTER_AUTH,
    priority = Integer.MAX_VALUE, // Run last to catch errors after all other interceptors
    enabledByDefault = true)
public class HtmlErrorResponseInterceptor implements WildcardInterceptor {

    @Inject("pebble-template-processor")
    private TemplateProcessor templateProcessor;

    /**
     * Handles the error response by delegating to shared error renderer.
     * Falls back to simple HTML if template processing fails.
     */
    @Override
    public void handle(final ServiceRequest<?> request, final ServiceResponse<?> response) {
        renderErrorPage(request, response, templateProcessor);
    }

    /**
     * Determines if this interceptor should handle the given request/response.
     *
     * @param request the service request
     * @param response the service response
     * @return true if the response is an error and the request accepts HTML, false otherwise
     */
    @Override
    public boolean resolve(final ServiceRequest<?> request, final ServiceResponse<?> response) {
        return response.isInError() && acceptsHtml(request.getHeaders());
    }

}
