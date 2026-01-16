package org.facet.templates;

import java.util.HashMap;
import java.util.Map;

import org.restheart.exchange.Request;

/**
 * Builder for template contexts, allowing addition of common data.
 */
public class TemplateContextBuilder {

    private final Map<String, Object> context;

    public TemplateContextBuilder(final Map<String, Object> globalContext) {
        this.context = new HashMap<>(globalContext);
    }

    /**
     * Creates a new TemplateContextBuilder with the given global context.
     *
     * @param globalContext the global template context
     * @return a new TemplateContextBuilder instance
     */
    public static TemplateContextBuilder create(final Map<String, Object> globalContext) {
        return new TemplateContextBuilder(globalContext != null ? globalContext : Map.of());
    }

    /**
     * Adds authenticated user information to the template context.
     *
     * @param request the service request
     * @return the updated TemplateContextBuilder
     */
    public TemplateContextBuilder withAuthenticatedUser(final Request<?> request) {
        final boolean isAuthenticated = request.getAuthenticatedAccount() != null;
        context.put("isAuthenticated", isAuthenticated);

        if (isAuthenticated) {
            context.put("username", request.getAuthenticatedAccount().getPrincipal().getName());
            if (request.getAuthenticatedAccount().getRoles() != null) {
                context.put("roles", request.getAuthenticatedAccount().getRoles());
            }
        }
        return this;
    }

    /**
     * Adds a key-value pair to the template context.
     *
     * @param key the context key
     * @param value the context value
     * @return the updated TemplateContextBuilder
     */
    public TemplateContextBuilder with(final String key, final Object value) {
        context.put(key, value);
        return this;
    }

    /**
     * Merges service data into the template context.
     * All keys from the service data map are added to the context.
     *
     * @param serviceData the service response data to merge
     * @return the updated TemplateContextBuilder
     */
    public TemplateContextBuilder withServiceData(final Map<String, Object> serviceData) {
        if (serviceData != null) {
            context.putAll(serviceData);
        }
        return this;
    }

    /**
     * Builds and returns the template context map.
     *
     * @return the constructed template context
     */
    public Map<String, Object> build() {
        return new HashMap<>(context);
    }
}
