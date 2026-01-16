package org.facet.html.handlers;

import java.util.Map;

import org.facet.html.HtmlResponseInterceptor;
import org.restheart.exchange.ServiceRequest;
import org.restheart.exchange.ServiceResponse;

/**
 * Strategy interface for handling different types of responses and converting them to HTML.
 *
 * <p>Implementations of this interface handle specific response types (MongoDB, JSON, GraphQL, etc.)
 * and build appropriate template contexts for HTML rendering.
 *
 * <h3>Handler Chain</h3>
 * <p>Handlers are checked in order until one returns true from {@link #canHandle(ServiceRequest, ServiceResponse)}.
 * The first matching handler builds the template context.
 *
 * <h3>Example Implementations</h3>
 * <ul>
 *   <li>{@code MongoHtmlResponseHandler} - MongoDB BSON responses with pagination</li>
 *   <li>{@code JsonHtmlResponseHandler} - Generic JSON responses (fallback)</li>
 *   <li>{@code GraphQLHtmlResponseHandler} - GraphQL responses (future)</li>
 * </ul>
 *
 * @see HtmlResponseInterceptor
 */
public interface HtmlResponseHandler {

    /**
     * Checks if this handler can process the given request/response pair.
     *
     * <p>Handlers are checked in priority order. The first handler that returns
     * true will be used to build the template context.
     *
     * @param request the service request
     * @param response the service response
     * @return true if this handler can process the response, false otherwise
     */
    boolean canHandle(ServiceRequest<?> request, ServiceResponse<?> response);

    /**
     * Builds the template context for rendering HTML from the response.
     *
     * <p>This method extracts data from the response and builds a context map
     * suitable for template rendering. The context should include:
     * <ul>
     *   <li>Authentication data (username, roles)</li>
     *   <li>Request metadata (path)</li>
     *   <li>Response data (documents, fields, etc.)</li>
     *   <li>Type-specific data (pagination for MongoDB, etc.)</li>
     * </ul>
     *
     * @param request the service request
     * @param response the service response
     * @return a map containing all template context variables
     */
    Map<String, Object> buildContext(ServiceRequest<?> request, ServiceResponse<?> response);
}
