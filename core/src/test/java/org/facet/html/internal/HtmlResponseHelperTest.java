package org.facet.html.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link HtmlResponseHelper}.
 */
class HtmlResponseHelperTest {

    // ──────────────────────────────────────────────────────────────────────
    // isEventStreamRequest
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isEventStreamRequest")
    class IsEventStreamRequest {

        @Test
        @DisplayName("Returns true for Accept: text/event-stream")
        void returnsTrueForEventStream() {
            HeaderMap headers = new HeaderMap();
            headers.add(Headers.ACCEPT, "text/event-stream");
            assertTrue(HtmlResponseHelper.isEventStreamRequest(headers));
        }

        @Test
        @DisplayName("Returns true for Accept: text/event-stream; charset=utf-8")
        void returnsTrueForEventStreamWithCharset() {
            HeaderMap headers = new HeaderMap();
            headers.add(Headers.ACCEPT, "text/event-stream; charset=utf-8");
            assertTrue(HtmlResponseHelper.isEventStreamRequest(headers));
        }

        @Test
        @DisplayName("Returns false for Accept: text/html")
        void returnsFalseForHtml() {
            HeaderMap headers = new HeaderMap();
            headers.add(Headers.ACCEPT, "text/html");
            assertFalse(HtmlResponseHelper.isEventStreamRequest(headers));
        }

        @Test
        @DisplayName("Returns false for Accept: */*")
        void returnsFalseForWildcard() {
            HeaderMap headers = new HeaderMap();
            headers.add(Headers.ACCEPT, "*/*");
            assertFalse(HtmlResponseHelper.isEventStreamRequest(headers));
        }

        @Test
        @DisplayName("Returns false when Accept header is absent")
        void returnsFalseWithNoAcceptHeader() {
            assertFalse(HtmlResponseHelper.isEventStreamRequest(new HeaderMap()));
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // acceptsHtml — regression: SSE must not be treated as HTML-capable
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("acceptsHtml — SSE regression")
    class AcceptsHtmlSseRegression {

        @Test
        @DisplayName("Returns false for Accept: text/event-stream (SSE must not be intercepted)")
        void returnsFalseForEventStream() {
            HeaderMap headers = new HeaderMap();
            headers.add(Headers.ACCEPT, "text/event-stream");
            assertFalse(HtmlResponseHelper.acceptsHtml(headers));
        }

        @Test
        @DisplayName("Returns true for Accept: text/html (unchanged behavior)")
        void returnsTrueForHtml() {
            HeaderMap headers = new HeaderMap();
            headers.add(Headers.ACCEPT, "text/html");
            assertTrue(HtmlResponseHelper.acceptsHtml(headers));
        }

        @Test
        @DisplayName("Returns true for HTMX request with HX-Request and Accept: */* (unchanged behavior)")
        void returnsTrueForHtmxRequest() {
            HeaderMap headers = new HeaderMap();
            headers.add(new io.undertow.util.HttpString("HX-Request"), "true");
            headers.add(Headers.ACCEPT, "*/*");
            assertTrue(HtmlResponseHelper.acceptsHtml(headers));
        }
    }
}
