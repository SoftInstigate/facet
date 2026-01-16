package org.facet.templates;

import java.util.Locale;
import java.util.Map;

/**
 * Interface for processing templates with context and locale support.
 */
public interface TemplateProcessor {

    /**
     * Process a template with the given context and locale.
     * 
     * @param template the template name
     * @param context the template context
     * @param locale the locale to use for processing
     * @return the processed template as a string
     * @throws TemplateProcessingException if template processing fails
     */
    String process(String template, Map<String, Object> context, Locale locale) throws TemplateProcessingException;

    /**
     * Process a template with the given context using the default locale.
     *
     * @param template the template name
     * @param context the template context
     * @return the processed template as a string
     * @throws TemplateProcessingException if template processing fails
     */
    default String process(String template, Map<String, Object> context) throws TemplateProcessingException {
        return process(template, context, null);
    }

    /**
     * Returns an immutable view of the global template context.
     * Use {@link #addToGlobalTemplateContext(String, Object)} to modify the global context.
     *
     * @return the global template context (read-only)
     */
    Map<String, Object> getGlobalTemplateContext();

    /**
     * Adds a key-value pair to the global template context.
     * The global context is shared across all template processing calls.
     *
     * @param key the context key
     * @param value the context value
     */
    void addToGlobalTemplateContext(String key, Object value);

    /**
     * Checks if a template exists and can be loaded.
     *
     * @param templateName the template name to check
     * @return true if the template exists, false otherwise
     */
    boolean templateExists(String templateName);

    void shutdown();

}
