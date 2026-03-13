package org.facet.html.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.restheart.exchange.ServiceRequest;

import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link HtmxRequestDetector}.
 */
class HtmxRequestDetectorTest {

    // ──────────────────────────────────────────────────────────────────────
    // isHtmxRequest(HeaderMap)
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isHtmxRequest(HeaderMap)")
    class IsHtmxRequestHeaderMap {

        @Test
        @DisplayName("Returns true when HX-Request present and Accept contains */*")
        void returnsTrueForHtmxRequest() {
            HeaderMap headers = buildHeaders(true, "*/*");
            assertTrue(HtmxRequestDetector.isHtmxRequest(headers));
        }

        @Test
        @DisplayName("Returns false when HX-Request absent")
        void returnsFalseWithoutHxRequest() {
            HeaderMap headers = new HeaderMap();
            headers.add(Headers.ACCEPT, "*/*");
            assertFalse(HtmxRequestDetector.isHtmxRequest(headers));
        }

        @Test
        @DisplayName("Returns false when Accept does not contain */*")
        void returnsFalseWithoutWildcardAccept() {
            HeaderMap headers = buildHeaders(true, "text/html");
            assertFalse(HtmxRequestDetector.isHtmxRequest(headers));
        }

        @Test
        @DisplayName("Returns false when neither header present")
        void returnsFalseWithNoHeaders() {
            assertFalse(HtmxRequestDetector.isHtmxRequest(new HeaderMap()));
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // isHtmxRequest(ServiceRequest)
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isHtmxRequest(ServiceRequest)")
    class IsHtmxRequestServiceRequest {

        @Test
        @DisplayName("Delegates to HeaderMap check")
        void delegatesToHeaderMap() {
            HeaderMap headers = buildHeaders(true, "*/*");
            ServiceRequest<?> request = mock(ServiceRequest.class);
            when(request.getHeaders()).thenReturn(headers);

            assertTrue(HtmxRequestDetector.isHtmxRequest(request));
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // getHxTarget / isTargeting
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isTargeting")
    class IsTargeting {

        @Test
        @DisplayName("Returns true when HX-Target matches (with # prefix)")
        void matchesWithHashPrefix() {
            ServiceRequest<?> request = htmxRequestWithTarget("#product-list");

            assertTrue(HtmxRequestDetector.isTargeting(request, "product-list"));
        }

        @Test
        @DisplayName("Returns true when HX-Target matches (without # prefix)")
        void matchesWithoutHashPrefix() {
            ServiceRequest<?> request = htmxRequestWithTarget("product-list");

            assertTrue(HtmxRequestDetector.isTargeting(request, "product-list"));
        }

        @Test
        @DisplayName("Returns false when HX-Target does not match")
        void returnsFalseForWrongTarget() {
            ServiceRequest<?> request = htmxRequestWithTarget("#other-element");

            assertFalse(HtmxRequestDetector.isTargeting(request, "product-list"));
        }

        @Test
        @DisplayName("Returns false when not an HTMX request")
        void returnsFalseForNonHtmx() {
            ServiceRequest<?> request = mock(ServiceRequest.class);
            HeaderMap headers = new HeaderMap();
            headers.add(Headers.ACCEPT, "text/html");
            when(request.getHeaders()).thenReturn(headers);
            when(request.getHeader("HX-Target")).thenReturn("#product-list");

            assertFalse(HtmxRequestDetector.isTargeting(request, "product-list"));
        }

        @Test
        @DisplayName("Returns false when HX-Target header is null")
        void returnsFalseWhenTargetNull() {
            ServiceRequest<?> request = mock(ServiceRequest.class);
            HeaderMap headers = buildHeaders(true, "*/*");
            when(request.getHeaders()).thenReturn(headers);
            when(request.getHeader("HX-Target")).thenReturn(null);

            assertFalse(HtmxRequestDetector.isTargeting(request, "product-list"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Getter methods
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Header getter methods")
    class HeaderGetters {

        @Test
        @DisplayName("getHxTrigger returns trigger header value")
        void getHxTriggerReturnsValue() {
            ServiceRequest<?> request = mock(ServiceRequest.class);
            when(request.getHeader("HX-Trigger")).thenReturn("submit-btn");

            assertEquals("submit-btn", HtmxRequestDetector.getHxTrigger(request));
        }

        @Test
        @DisplayName("getHxTriggerName returns trigger name")
        void getHxTriggerNameReturnsValue() {
            ServiceRequest<?> request = mock(ServiceRequest.class);
            when(request.getHeader("HX-Trigger-Name")).thenReturn("search");

            assertEquals("search", HtmxRequestDetector.getHxTriggerName(request));
        }

        @Test
        @DisplayName("getHxCurrentUrl returns current URL")
        void getHxCurrentUrlReturnsValue() {
            ServiceRequest<?> request = mock(ServiceRequest.class);
            when(request.getHeader("HX-Current-URL")).thenReturn("http://localhost:8080/products");

            assertEquals("http://localhost:8080/products", HtmxRequestDetector.getHxCurrentUrl(request));
        }

        @Test
        @DisplayName("getHxPrompt returns prompt response")
        void getHxPromptReturnsValue() {
            ServiceRequest<?> request = mock(ServiceRequest.class);
            when(request.getHeader("HX-Prompt")).thenReturn("yes");

            assertEquals("yes", HtmxRequestDetector.getHxPrompt(request));
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────

    private HeaderMap buildHeaders(final boolean addHxRequest, final String acceptValue) {
        final HeaderMap headers = new HeaderMap();
        if (addHxRequest) {
            headers.add(new HttpString("HX-Request"), "true");
        }
        headers.add(Headers.ACCEPT, acceptValue);
        return headers;
    }

    private ServiceRequest<?> htmxRequestWithTarget(final String target) {
        ServiceRequest<?> request = mock(ServiceRequest.class);
        HeaderMap headers = buildHeaders(true, "*/*");
        when(request.getHeaders()).thenReturn(headers);
        when(request.getHeader("HX-Target")).thenReturn(target);
        return request;
    }
}
