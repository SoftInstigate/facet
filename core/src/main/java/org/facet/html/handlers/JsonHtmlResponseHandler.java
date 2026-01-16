package org.facet.html.handlers;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.facet.templates.TemplateContextBuilder;
import org.facet.templates.TemplateProcessor;
import org.restheart.exchange.ServiceRequest;
import org.restheart.exchange.ServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * Handler for generic JSON responses.
 *
 * <p>
 * This handler processes standard JSON responses from any service and builds
 * simple template contexts with:
 * <ul>
 * <li>Authentication data (username, roles)</li>
 * <li>Request metadata (path)</li>
 * <li>Service response data (parsed from JSON)</li>
 * </ul>
 *
 * <p>
 * This is a fallback handler that matches any response. It should be registered
 * last in the handler chain so more specific handlers (like MongoDB) are checked first.
 *
 * @see HtmlResponseHandler
 */
public class JsonHtmlResponseHandler implements HtmlResponseHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonHtmlResponseHandler.class);
    private static final Gson GSON = new Gson();

    private final TemplateProcessor templateProcessor;

    /**
     * Creates a new JsonHtmlResponseHandler.
     *
     * @param templateProcessor the template processor for global context
     */
    public JsonHtmlResponseHandler(final TemplateProcessor templateProcessor) {
        this.templateProcessor = templateProcessor;
    }

    /**
     * This handler accepts any request/response as a fallback.
     * It should be registered last in the handler chain.
     */
    @Override
    public boolean canHandle(final ServiceRequest<?> request, final ServiceResponse<?> response) {
        // Fallback handler - accepts anything
        return true;
    }

    /**
     * Builds template context with generic JSON data.
     */
    @Override
    public Map<String, Object> buildContext(final ServiceRequest<?> request, final ServiceResponse<?> response) {

        LOGGER.trace("Building generic JSON template context for path: {}", request.getPath());

        // Extract content once and reuse for parsing + raw JSON template variable
        final String content = getContentString(response);
        final var serviceData = parseResponseDataFromString(content);
        final String rawJson = content;

        // Build context
        return TemplateContextBuilder.create(templateProcessor.getGlobalTemplateContext())
                .withAuthenticatedUser(request)
                .with("path", request.getPath())
                .with("json", rawJson)
                .withServiceData(serviceData)
                .build();
    }

    private String getContentString(final ServiceResponse<?> response) {
        try {
            final Object contentObj = response.getContent();
            if (contentObj == null) {
                return "{}";
            } else if (contentObj instanceof byte[] byteArray) {
                return new String(byteArray, StandardCharsets.UTF_8);
            } else if (contentObj instanceof ByteBuffer byteBuffer) {
                final ByteBuffer bb = byteBuffer;
                final byte[] bytes = new byte[bb.remaining()];
                bb.get(bytes);
                return new String(bytes, StandardCharsets.UTF_8);
            } else if (contentObj instanceof InputStream inputStream) {
                try (final InputStream in = inputStream) {
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            } else {
                return contentObj.toString();
            }
        } catch (final Exception e) {
            LOGGER.debug("Failed to extract raw JSON string from response: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * Parses a JSON string into a Map for template contexts.
     *
     * @param content the JSON content as string
     * @return map of response data
     */
    private Map<String, Object> parseResponseDataFromString(final String content) {
        try {
            final JsonElement jsonElement = JsonParser.parseString(content == null || content.isBlank() ? "{}" : content);

            @SuppressWarnings("unchecked")
            final Map<String, Object> dataMap = GSON.fromJson(jsonElement, Map.class);

            return dataMap != null ? dataMap : Map.of();
        } catch (final Exception e) {
            LOGGER.warn("Failed to parse response content as JSON", e);
            return Map.of();
        }
    }
}
