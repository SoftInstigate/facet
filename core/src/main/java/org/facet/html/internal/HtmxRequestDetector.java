package org.facet.html.internal;

import org.restheart.exchange.ServiceRequest;

import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;

/**
 * Utility class for detecting and extracting HTMX-specific request headers.
 *
 * HTMX sends special headers with every request to help the server distinguish
 * between full page loads and partial content updates (fragments).
 *
 * Key HTMX Headers:
 * - HX-Request: Always "true" for HTMX requests
 * - HX-Target: CSS selector of the target element
 * - HX-Trigger: ID of the element that triggered the request
 * - HX-Trigger-Name: Name attribute of the triggering element
 * - HX-Current-URL: Current URL of the browser
 * - HX-Prompt: User response to hx-prompt
 *
 * @see <a href="https://htmx.org/reference/#request_headers">HTMX Request Headers</a>
 */
public class HtmxRequestDetector {

    // HTMX request header constants
    private static final String HX_REQUEST = "HX-Request";
    private static final String HX_TARGET = "HX-Target";
    private static final String HX_TRIGGER = "HX-Trigger";
    private static final String HX_TRIGGER_NAME = "HX-Trigger-Name";
    private static final String HX_CURRENT_URL = "HX-Current-URL";
    private static final String HX_PROMPT = "HX-Prompt";

    private HtmxRequestDetector() {
        // Utility class, no instantiation
    }

    /**
     * Checks if the request originated from HTMX.
     *
     * HTMX sets the HX-Request header to "true" and sends Accept: &#42;/&#42; for all its requests.
     * This allows the server to distinguish between:
     * - Full page loads (browser navigation)
     * - Partial updates (HTMX fragment swaps)
     *
     * @param request The service request to check
     * @return true if this is an HTMX request, false otherwise
     */
    public static boolean isHtmxRequest(final ServiceRequest<?> request) {
        return isHtmxRequest(request.getHeaders());
    }

    /**
     * Checks if the request originated from HTMX.
     *
     * HTMX sets the HX-Request header to "true" and sends Accept: &#42;/&#42; for all its requests.
     * This allows the server to distinguish between:
     * - Full page loads (browser navigation)
     * - Partial updates (HTMX fragment swaps)
     *
     * @param headers The request headers to check
     * @return true if this is an HTMX request (HX-Request: true AND Accept: &#42;/&#42;), false otherwise
     */
    public static boolean isHtmxRequest(final HeaderMap headers) {
        return headers.contains(HX_REQUEST)
                && headers.contains(Headers.ACCEPT)
                && headers.get(Headers.ACCEPT).stream().anyMatch(v -> v.contains("*/*"));
    }

    /**
     * Gets the CSS selector of the target element that will be updated.
     *
     * This corresponds to the hx-target attribute in the HTML.
     * Example: "#document-list", ".pagination-container"
     *
     * @param request The service request
     * @return The target selector, or null if not present
     */
    public static String getHxTarget(final ServiceRequest<?> request) {
        return request.getHeader(HX_TARGET);
    }

    /**
     * Gets the ID of the element that triggered the request.
     *
     * This corresponds to the id attribute of the element with hx-* attributes.
     * Example: "next-page-button", "search-form"
     *
     * @param request The service request
     * @return The trigger element ID, or null if not present
     */
    public static String getHxTrigger(final ServiceRequest<?> request) {
        return request.getHeader(HX_TRIGGER);
    }

    /**
     * Gets the name attribute of the element that triggered the request.
     *
     * Useful for forms where elements have name attributes.
     * Example: "filter", "pagesize"
     *
     * @param request The service request
     * @return The trigger element name, or null if not present
     */
    public static String getHxTriggerName(final ServiceRequest<?> request) {
        return request.getHeader(HX_TRIGGER_NAME);
    }

    /**
     * Gets the current URL of the browser when the request was made.
     *
     * Useful for context-aware responses that depend on the current page.
     *
     * @param request The service request
     * @return The current browser URL, or null if not present
     */
    public static String getHxCurrentUrl(final ServiceRequest<?> request) {
        return request.getHeader(HX_CURRENT_URL);
    }

    /**
     * Gets the user's response to an hx-prompt.
     *
     * If the element has an hx-prompt attribute, HTMX will show a prompt
     * and send the user's response in this header.
     *
     * @param request The service request
     * @return The prompt response, or null if not present
     */
    public static String getHxPrompt(final ServiceRequest<?> request) {
        return request.getHeader(HX_PROMPT);
    }

    /**
     * Checks if this HTMX request is targeting a specific element by ID.
     *
     * @param request The service request
     * @param targetId The target element ID to check (e.g., "document-list")
     * @return true if the target matches, false otherwise
     */
    public static boolean isTargeting(final ServiceRequest<?> request, final String targetId) {
        if (!isHtmxRequest(request)) {
            return false;
        }

        final String target = getHxTarget(request);
        if (target == null) {
            return false;
        }

        // Handle both "#id" and "id" formats
        final String normalizedTarget = target.startsWith("#")
            ? target.substring(1)
            : target;
        return normalizedTarget.equals(targetId);
    }
}
