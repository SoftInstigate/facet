package org.facet.templates.pebble;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.facet.templates.TemplateProcessingException;
import org.facet.templates.TemplateProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import io.pebbletemplates.pebble.loader.FileLoader;
import io.pebbletemplates.pebble.loader.Loader;
import io.pebbletemplates.pebble.template.PebbleTemplate;

/**
 * Implementation of TemplateProcessor using Pebble templating engine.
 */
class PebbleTemplateProcessor implements TemplateProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PebbleTemplateProcessor.class);

    private static final String DEFAULT_TEMPLATES_FOLDER = "templates/";

    private final PebbleEngine engine;
    private final ExecutorService executorService;
    private final Map<String, Object> globalTemplateContext;
    /**
     * If true, templates are loaded from the filesystem (useful for development).
     */
    private final boolean useFileLoader;
    /**
     * If true, enable template caching in the Pebble engine.
     */
    private final boolean cacheActive;
    /**
     * Custom filesystem path for templates (when useFileLoader is true).
     */
    private final String templatesPath;

    /**
     * Constructs a PebbleTemplateProcessor.
     *
     * @param initialTemplateContext the initial global template context
     * @param useFileLoader          if true, load templates from the filesystem
     *                               instead of the classpath
     * @param cacheActive            if true, enable Pebble template caching
     * @param defaultLocale          the default locale for template processing
     * @param templatesPath          custom filesystem path for templates (only used
     *                               when useFileLoader is true)
     */
    public PebbleTemplateProcessor(final Map<String, Object> initialTemplateContext,
            final boolean useFileLoader, final boolean cacheActive, final Locale defaultLocale,
            final String templatesPath) {
        globalTemplateContext = new HashMap<>(initialTemplateContext);

        this.useFileLoader = useFileLoader;
        this.cacheActive = cacheActive;
        final String basePath = templatesPath != null ? templatesPath
                : "src/main/resources/" + DEFAULT_TEMPLATES_FOLDER;
        this.templatesPath = new File(basePath).getAbsolutePath();

        LOGGER.info("Pebble template processor initialized (fileLoader: {}, cacheActive: {}, templatesPath: {})",
                useFileLoader, cacheActive,
                useFileLoader ? this.templatesPath : "classpath:" + DEFAULT_TEMPLATES_FOLDER);

        // Create executor service for parallel template rendering
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        this.engine = builder(defaultLocale).executorService(executorService).build();

        // Add shutdown hook to ensure proper cleanup on JVM exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("JVM shutdown detected");
            shutdown();
        }, "PebbleTemplateProcessor-shutdown-hook"));
    }

    /**
     * Checks if a template exists and can be loaded by the Pebble engine.
     *
     * @param templateName the template name to check
     * @return true if the template exists and can be loaded, false otherwise
     */
    @Override
    public boolean templateExists(final String templateName) {
        try {
            engine.getTemplate(templateName);
            LOGGER.trace("Template '{}' exists and is valid", templateName);
            return true;
        } catch (final Exception e) {
            LOGGER.warn("Template '{}' check failed: {} - {}", templateName, e.getClass().getSimpleName(),
                    e.getMessage());
            return false;
        }
    }

    /**
     * Shuts down the PebbleTemplateProcessor, releasing resources.
     */
    @Override
    public void shutdown() {
        LOGGER.info("Shutting down");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (final InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Processes a template with the given context and locale.
     * 
     * @param template the template name
     * @param context  the template context
     * @param locale   the locale to use for processing
     * @return the processed template as a string
     * @throws TemplateProcessingException if template processing fails
     */
    @Override
    public String process(final String template, final Map<String, Object> context, final Locale locale)
            throws TemplateProcessingException {
        LOGGER.debug("Processing template: {}", template);
        final PebbleTemplate compiledTemplate = engine.getTemplate(template);
        final var writer = new StringWriter();
        try {
            if (context == null && locale == null) {
                compiledTemplate.evaluate(writer);
            } else if (context == null) {
                compiledTemplate.evaluate(writer, locale);
            } else if (locale == null) {
                compiledTemplate.evaluate(writer, context);
            } else {
                compiledTemplate.evaluate(writer, context, locale);
            }
        } catch (final IOException e) {
            throw new TemplateProcessingException("Error processing template: " + template, e);
        }
        return writer.toString();
    }

    /**
     * Returns an unmodifiable copy of the global template context.
     *
     * @return the global template context (read-only)
     */
    @Override
    public Map<String, Object> getGlobalTemplateContext() {
        return Map.copyOf(this.globalTemplateContext);
    }

    /**
     * Adds a key-value pair to the global template context.
     * The global context is shared across all template processing calls.
     *
     * @param key   the context key
     * @param value the context value
     */
    @Override
    public void addToGlobalTemplateContext(final String key, final Object value) {
        this.globalTemplateContext.put(key, value);
        LOGGER.debug("Added to global template context: {} = {}", key, value);
    }

    /**
     * Builds the Pebble engine builder with appropriate loader and settings.
     * 
     * @param defaultLocale the default locale for the engine
     * @return the PebbleEngine.Builder instance
     */
    private PebbleEngine.Builder builder(final Locale defaultLocale) {
        final PebbleEngine.Builder builder = new PebbleEngine.Builder();
        final Loader<String> loader;

        if (useFileLoader) {
            loader = new FileLoader(templatesPath);
            LOGGER.debug("Using file loader for templates (filesystem: {})", templatesPath);
        } else {
            loader = new ClasspathLoader();
            loader.setPrefix(DEFAULT_TEMPLATES_FOLDER);
            LOGGER.debug("Using classpath loader for templates (classpath: {})", DEFAULT_TEMPLATES_FOLDER);
        }

        loader.setSuffix(".html");
        builder.loader(loader)
                .cacheActive(cacheActive)
                .defaultLocale(defaultLocale)
                .extension(new CustomPebbleExtension());

        LOGGER.debug("Using locale: {}", defaultLocale);

        return builder;
    }
}
