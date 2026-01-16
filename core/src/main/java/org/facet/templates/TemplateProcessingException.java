package org.facet.templates;

/**
 * Exception thrown when template processing fails.
 */
public class TemplateProcessingException extends Exception {

    public TemplateProcessingException(final String message) {
        super(message);
    }

    public TemplateProcessingException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public TemplateProcessingException(final Throwable cause) {
        super(cause);
    }

}
