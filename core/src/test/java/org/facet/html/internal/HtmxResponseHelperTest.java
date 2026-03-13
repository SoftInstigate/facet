package org.facet.html.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.restheart.exchange.ServiceResponse;

import com.google.gson.JsonObject;

import static org.mockito.Mockito.*;

/**
 * Tests for {@link HtmxResponseHelper} — verifies that each method sets the
 * correct HTMX response header with the expected value.
 */
class HtmxResponseHelperTest {

    private final ServiceResponse<?> response = mock(ServiceResponse.class);

    // ── Event triggering ──────────────────────────────────────────────────

    @Test
    @DisplayName("triggerEvent sets HX-Trigger with event name")
    void triggerEventSetsHeader() {
        HtmxResponseHelper.triggerEvent(response, "productDeleted");
        verify(response).setHeader("HX-Trigger", "productDeleted");
    }

    @Test
    @DisplayName("triggerEvent with details sets HX-Trigger as JSON")
    void triggerEventWithDetailsSetsJson() {
        final var details = new JsonObject();
        details.addProperty("id", "123");

        HtmxResponseHelper.triggerEvent(response, "productUpdated", details);

        verify(response).setHeader(eq("HX-Trigger"), argThat(val ->
                val.contains("productUpdated") && val.contains("123")));
    }

    @Test
    @DisplayName("triggerEventAfterSwap sets HX-Trigger-After-Swap")
    void triggerEventAfterSwapSetsHeader() {
        HtmxResponseHelper.triggerEventAfterSwap(response, "listRefreshed");
        verify(response).setHeader("HX-Trigger-After-Swap", "listRefreshed");
    }

    @Test
    @DisplayName("triggerEventAfterSwap with details sets HX-Trigger-After-Swap as JSON")
    void triggerEventAfterSwapWithDetailsSetsJson() {
        final var details = new JsonObject();
        details.addProperty("count", 5);

        HtmxResponseHelper.triggerEventAfterSwap(response, "itemsLoaded", details);

        verify(response).setHeader(eq("HX-Trigger-After-Swap"), argThat(val ->
                val.contains("itemsLoaded") && val.contains("5")));
    }

    @Test
    @DisplayName("triggerEventAfterSettle sets HX-Trigger-After-Settle")
    void triggerEventAfterSettleSetsHeader() {
        HtmxResponseHelper.triggerEventAfterSettle(response, "animationDone");
        verify(response).setHeader("HX-Trigger-After-Settle", "animationDone");
    }

    @Test
    @DisplayName("triggerEventAfterSettle with details sets HX-Trigger-After-Settle as JSON")
    void triggerEventAfterSettleWithDetailsSetsJson() {
        final var details = new JsonObject();
        details.addProperty("ok", true);

        HtmxResponseHelper.triggerEventAfterSettle(response, "settled", details);

        verify(response).setHeader(eq("HX-Trigger-After-Settle"), argThat(val ->
                val.contains("settled") && val.contains("true")));
    }

    // ── Swap control ──────────────────────────────────────────────────────

    @Test
    @DisplayName("retarget sets HX-Retarget header")
    void retargetSetsHeader() {
        HtmxResponseHelper.retarget(response, "#notifications");
        verify(response).setHeader("HX-Retarget", "#notifications");
    }

    @Test
    @DisplayName("reswap sets HX-Reswap header")
    void reswapSetsHeader() {
        HtmxResponseHelper.reswap(response, "beforeend");
        verify(response).setHeader("HX-Reswap", "beforeend");
    }

    // ── Browser history ───────────────────────────────────────────────────

    @Test
    @DisplayName("pushUrl sets HX-Push-Url header")
    void pushUrlSetsHeader() {
        HtmxResponseHelper.pushUrl(response, "/products/new");
        verify(response).setHeader("HX-Push-Url", "/products/new");
    }

    @Test
    @DisplayName("replaceUrl sets HX-Replace-Url header")
    void replaceUrlSetsHeader() {
        HtmxResponseHelper.replaceUrl(response, "/products");
        verify(response).setHeader("HX-Replace-Url", "/products");
    }

    // ── Navigation ────────────────────────────────────────────────────────

    @Test
    @DisplayName("redirect sets HX-Redirect header")
    void redirectSetsHeader() {
        HtmxResponseHelper.redirect(response, "/login");
        verify(response).setHeader("HX-Redirect", "/login");
    }

    @Test
    @DisplayName("refresh sets HX-Refresh: true")
    void refreshSetsHeader() {
        HtmxResponseHelper.refresh(response);
        verify(response).setHeader("HX-Refresh", "true");
    }
}
