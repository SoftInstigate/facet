package org.facet.html.handlers;

import java.util.Map;

import org.facet.templates.TemplateProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.restheart.exchange.ExchangeKeys.METHOD;
import org.restheart.exchange.ServiceRequest;
import org.restheart.exchange.ServiceResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link JsonHtmlResponseHandler}.
 */
class JsonHtmlResponseHandlerTest {

    private static final String JSON =
            "{\"username\":\"spoofed\",\"isAuthenticated\":true,\"path\":\"/x\",\"foo\":\"bar\"}";

    @Test
    @DisplayName("Service response fields cannot override Facet-managed variables")
    void serviceDataCannotSpoofAuthOrPath() {
        TemplateProcessor tp = mock(TemplateProcessor.class);
        when(tp.getGlobalTemplateContext()).thenReturn(Map.of());

        ServiceRequest<?> request = mock(ServiceRequest.class);
        when(request.getAuthenticatedAccount()).thenReturn(null);
        when(request.getMethod()).thenReturn(METHOD.GET);
        when(request.getPath()).thenReturn("/svc");

        ServiceResponse<?> response = mock(ServiceResponse.class);
        when(response.getContent()).thenReturn(JSON);

        Map<String, Object> ctx = new JsonHtmlResponseHandler(tp).buildContext(request, response);

        assertEquals(Boolean.FALSE, ctx.get("isAuthenticated"));
        assertEquals("/svc", ctx.get("path"));
        assertEquals("bar", ctx.get("foo"));
        assertEquals(JSON, ctx.get("data"));
    }

    @Test
    @DisplayName("Unauthenticated request does not expose a spoofed username")
    void usernameNotSpoofed() {
        TemplateProcessor tp = mock(TemplateProcessor.class);
        when(tp.getGlobalTemplateContext()).thenReturn(Map.of());
        ServiceRequest<?> request = mock(ServiceRequest.class);
        when(request.getAuthenticatedAccount()).thenReturn(null);
        when(request.getMethod()).thenReturn(METHOD.GET);
        when(request.getPath()).thenReturn("/svc");
        ServiceResponse<?> response = mock(ServiceResponse.class);
        when(response.getContent()).thenReturn(JSON);

        Map<String, Object> ctx = new JsonHtmlResponseHandler(tp).buildContext(request, response);
        assertNotEquals("spoofed", ctx.get("username"));
    }
}
