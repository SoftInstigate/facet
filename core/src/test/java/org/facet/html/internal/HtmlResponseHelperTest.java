package org.facet.html.internal;

import java.util.HashMap;
import java.util.Map;

import org.facet.templates.TemplateProcessingException;
import org.facet.templates.TemplateProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;

import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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

    // ──────────────────────────────────────────────────────────────────────
    // renderErrorHtml
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("renderErrorHtml")
    class RenderErrorHtml {

        private TemplateProcessor tp() {
            TemplateProcessor tp = mock(TemplateProcessor.class);
            when(tp.getGlobalTemplateContext()).thenReturn(Map.of());
            return tp;
        }

        @Test
        @DisplayName("No error template returns built-in fallback")
        void noTemplateReturnsFallback() throws TemplateProcessingException {
            TemplateProcessor tp = tp();
            String html = HtmlResponseHelper.renderErrorHtml(tp, 404, "/x", null);
            assertTrue(html.contains("Error 404"));
            verify(tp, never()).process(anyString(), anyMap());
        }

        @Test
        @DisplayName("Status-specific template is processed and returned")
        void specificTemplate() throws TemplateProcessingException {
            TemplateProcessor tp = tp();
            when(tp.templateExists("errors/404")).thenReturn(true);
            when(tp.process(eq("errors/404"), anyMap())).thenReturn("custom404");
            assertEquals("custom404", HtmlResponseHelper.renderErrorHtml(tp, 404, "/x", "bob"));
            verify(tp).process(eq("errors/404"), anyMap());
        }

        @Test
        @DisplayName("Generic error template is used when no specific one exists")
        void genericTemplate() throws TemplateProcessingException {
            TemplateProcessor tp = tp();
            when(tp.templateExists("error")).thenReturn(true);
            when(tp.process(eq("error"), anyMap())).thenReturn("generic");
            assertEquals("generic", HtmlResponseHelper.renderErrorHtml(tp, 500, "/x", null));
        }

        @Test
        @DisplayName("TemplateProcessingException yields fallback")
        void processingExceptionFallback() throws TemplateProcessingException {
            TemplateProcessor tp = tp();
            when(tp.templateExists("errors/404")).thenReturn(true);
            when(tp.process(eq("errors/404"), anyMap())).thenThrow(new TemplateProcessingException("boom"));
            assertTrue(HtmlResponseHelper.renderErrorHtml(tp, 404, "/x", null).contains("Error 404"));
        }

        @Test
        @DisplayName("RuntimeException yields fallback")
        void runtimeExceptionFallback() throws TemplateProcessingException {
            TemplateProcessor tp = tp();
            when(tp.templateExists("errors/404")).thenReturn(true);
            when(tp.process(eq("errors/404"), anyMap())).thenThrow(new IllegalStateException("boom"));
            assertTrue(HtmlResponseHelper.renderErrorHtml(tp, 404, "/x", null).contains("Error 404"));
        }

        @Test
        @DisplayName("Null processor yields fallback")
        void nullProcessor() {
            assertTrue(HtmlResponseHelper.renderErrorHtml(null, 404, "/x", null).contains("Error 404"));
        }

        @Test
        @DisplayName("Context contains statusCode, statusMessage, path and username")
        void contextContents() throws TemplateProcessingException {
            TemplateProcessor tp = tp();
            when(tp.templateExists("errors/403")).thenReturn(true);
            when(tp.process(eq("errors/403"), anyMap())).thenReturn("ok");
            HtmlResponseHelper.renderErrorHtml(tp, 403, "/secret", "alice");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            verify(tp).process(eq("errors/403"), captor.capture());
            Map<String, Object> ctx = captor.getValue();
            assertEquals(403, ctx.get("statusCode"));
            assertNotNull(ctx.get("statusMessage"));
            assertEquals("/secret", ctx.get("path"));
            assertEquals("alice", ctx.get("username"));
        }
    }
}
