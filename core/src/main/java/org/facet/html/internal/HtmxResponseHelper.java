package org.facet.html.internal;

import org.restheart.exchange.ServiceResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Utility class for setting HTMX-specific response headers.
 *
 * <p>HTMX provides several response headers that allow the server to control
 * client-side behavior beyond just swapping content. This helper provides type-safe
 * methods for setting these headers.</p>
 *
 * <h3>Available Response Headers</h3>
 * <ul>
 *   <li><strong>HX-Trigger</strong> - Trigger client-side events immediately</li>
 *   <li><strong>HX-Trigger-After-Swap</strong> - Trigger events after content swap</li>
 *   <li><strong>HX-Trigger-After-Settle</strong> - Trigger events after DOM settles</li>
 *   <li><strong>HX-Retarget</strong> - Change the target element selector</li>
 *   <li><strong>HX-Reswap</strong> - Change the swap strategy (innerHTML, outerHTML, etc.)</li>
 *   <li><strong>HX-Push-Url</strong> - Push a new URL to browser history</li>
 *   <li><strong>HX-Replace-Url</strong> - Replace current URL in browser history</li>
 *   <li><strong>HX-Redirect</strong> - Client-side redirect</li>
 *   <li><strong>HX-Refresh</strong> - Force full page refresh</li>
 * </ul>
 *
 * <h3>Usage Examples</h3>
 * <pre>{@code
 * // Trigger a simple event
 * HtmxResponseHelper.triggerEvent(response, "itemDeleted");
 *
 * // Trigger event with details
 * var details = new JsonObject();
 * details.addProperty("id", "123");
 * HtmxResponseHelper.triggerEventAfterSwap(response, "itemUpdated", details);
 *
 * // Change swap behavior dynamically
 * HtmxResponseHelper.retarget(response, "#notifications");
 * HtmxResponseHelper.reswap(response, "beforeend");
 *
 * // Update browser history
 * HtmxResponseHelper.pushUrl(response, "/new/path");
 * }</pre>
 *
 * @see <a href="https://htmx.org/reference/#response_headers">HTMX Response Headers Reference</a>
 */
public class HtmxResponseHelper {

    private static final String HX_TRIGGER = "HX-Trigger";
    private static final String HX_TRIGGER_AFTER_SWAP = "HX-Trigger-After-Swap";
    private static final String HX_TRIGGER_AFTER_SETTLE = "HX-Trigger-After-Settle";
    private static final String HX_RETARGET = "HX-Retarget";
    private static final String HX_RESWAP = "HX-Reswap";
    private static final String HX_PUSH_URL = "HX-Push-Url";
    private static final String HX_REPLACE_URL = "HX-Replace-Url";
    private static final String HX_REDIRECT = "HX-Redirect";
    private static final String HX_REFRESH = "HX-Refresh";

    private static final Gson GSON = new Gson();

    private HtmxResponseHelper() {
        // Utility class
    }

    // ==================== Event Triggering ====================

    /**
     * Triggers a client-side event immediately when the response is received.
     *
     * @param response the service response
     * @param eventName the name of the event to trigger
     */
    public static void triggerEvent(final ServiceResponse<?> response, final String eventName) {
        response.setHeader(HX_TRIGGER, eventName);
    }

    /**
     * Triggers a client-side event with details immediately when the response is received.
     *
     * @param response the service response
     * @param eventName the name of the event to trigger
     * @param details JSON object with event details
     */
    public static void triggerEvent(final ServiceResponse<?> response, final String eventName, final JsonObject details) {
        final var wrapper = new JsonObject();
        wrapper.add(eventName, details);
        response.setHeader(HX_TRIGGER, GSON.toJson(wrapper));
    }

    /**
     * Triggers a client-side event after the content swap phase.
     *
     * @param response the service response
     * @param eventName the name of the event to trigger
     */
    public static void triggerEventAfterSwap(final ServiceResponse<?> response, final String eventName) {
        response.setHeader(HX_TRIGGER_AFTER_SWAP, eventName);
    }

    /**
     * Triggers a client-side event with details after the content swap phase.
     *
     * @param response the service response
     * @param eventName the name of the event to trigger
     * @param details JSON object with event details
     */
    public static void triggerEventAfterSwap(final ServiceResponse<?> response, final String eventName, final JsonObject details) {
        final var wrapper = new JsonObject();
        wrapper.add(eventName, details);
        response.setHeader(HX_TRIGGER_AFTER_SWAP, GSON.toJson(wrapper));
    }

    /**
     * Triggers a client-side event after the settle phase (after DOM animations complete).
     *
     * @param response the service response
     * @param eventName the name of the event to trigger
     */
    public static void triggerEventAfterSettle(final ServiceResponse<?> response, final String eventName) {
        response.setHeader(HX_TRIGGER_AFTER_SETTLE, eventName);
    }

    /**
     * Triggers a client-side event with details after the settle phase.
     *
     * @param response the service response
     * @param eventName the name of the event to trigger
     * @param details JSON object with event details
     */
    public static void triggerEventAfterSettle(final ServiceResponse<?> response, final String eventName, final JsonObject details) {
        final var wrapper = new JsonObject();
        wrapper.add(eventName, details);
        response.setHeader(HX_TRIGGER_AFTER_SETTLE, GSON.toJson(wrapper));
    }

    // ==================== Swap Control ====================

    /**
     * Changes the target element for the content swap.
     *
     * <p>Example: {@code retarget(response, "#notifications")} will swap content
     * into the element with ID "notifications" instead of the original target.</p>
     *
     * @param response the service response
     * @param cssSelector the CSS selector for the new target element
     */
    public static void retarget(final ServiceResponse<?> response, final String cssSelector) {
        response.setHeader(HX_RETARGET, cssSelector);
    }

    /**
     * Changes the swap strategy for the response.
     *
     * <p>Valid values: innerHTML, outerHTML, beforebegin, afterbegin, beforeend, afterend, delete, none</p>
     *
     * @param response the service response
     * @param swapStrategy the swap strategy (e.g., "innerHTML", "beforeend")
     */
    public static void reswap(final ServiceResponse<?> response, final String swapStrategy) {
        response.setHeader(HX_RESWAP, swapStrategy);
    }

    // ==================== Browser History ====================

    /**
     * Pushes a new URL into the browser history stack.
     *
     * <p>Use this to update the browser's address bar without a full page reload.
     * The URL will be added to history, allowing back/forward navigation.</p>
     *
     * @param response the service response
     * @param url the URL to push
     */
    public static void pushUrl(final ServiceResponse<?> response, final String url) {
        response.setHeader(HX_PUSH_URL, url);
    }

    /**
     * Replaces the current URL in the browser history.
     *
     * <p>Use this to update the address bar without adding a new history entry.
     * Use "false" to prevent URL update entirely.</p>
     *
     * @param response the service response
     * @param url the URL to replace with, or "false" to prevent update
     */
    public static void replaceUrl(final ServiceResponse<?> response, final String url) {
        response.setHeader(HX_REPLACE_URL, url);
    }

    // ==================== Navigation ====================

    /**
     * Performs a client-side redirect to the specified URL.
     *
     * <p>This is a full page redirect, similar to {@code window.location.href = url}.</p>
     *
     * @param response the service response
     * @param url the URL to redirect to
     */
    public static void redirect(final ServiceResponse<?> response, final String url) {
        response.setHeader(HX_REDIRECT, url);
    }

    /**
     * Forces a full page refresh on the client.
     *
     * <p>Use this when the server state has changed in a way that requires
     * reloading the entire page (e.g., session expired, major data changes).</p>
     *
     * @param response the service response
     */
    public static void refresh(final ServiceResponse<?> response) {
        response.setHeader(HX_REFRESH, "true");
    }
}
