package org.facet.templates.pebble;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.facet.templates.TemplateProcessor;
import org.restheart.Version;
import org.restheart.plugins.Inject;
import org.restheart.plugins.OnInit;
import org.restheart.plugins.PluginRecord;
import org.restheart.plugins.Provider;
import org.restheart.plugins.RegisterPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider for PebbleTemplateProcessor.
 *
 * <h3>Configuration</h3>
 * 
 * <pre>
 * /pebble-template-processor:
 *   enabled: true
 *   # Load templates from filesystem (for hot-reload during development)
 *   use-file-loader: false           # Optional: default is false (classpath)
 *   # Custom filesystem path for templates (only used when use-file-loader is true)
 *   templates-path: /opt/templates/  # Optional: default is "src/main/resources/templates/"
 *   # Enable Pebble template caching
 *   cache-active: true               # Optional: default is true (enabled)
 *   locale: en-US                    # Optional: ISO language tag (defaults to server locale)
 * </pre>
 *
 * <p>
 * <strong>Required by:</strong> RESTHeart Browser, HTML error pages
 * </p>
 * <p>
 * <strong>Used by:</strong> BrowserService (including error page rendering), MongoHtmlResponseInterceptor
 * </p>
 */
@RegisterPlugin(
    name = "pebble-template-processor",
    description = "Provides a Pebble template processor for HTML rendering",
    enabledByDefault = false)
public class PebbleTemplateProcessorProvider implements Provider<TemplateProcessor> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PebbleTemplateProcessorProvider.class);

    @Inject("config")
    private Map<String, Object> config;

    private TemplateProcessor templateProcessor;

    /**
     * Initializes the PebbleTemplateProcessor.
     */
    @OnInit
    public void onInit() {
        final var globalTemplateContext = new HashMap<String, Object>();

        final String version = Version.getInstance().getVersionNumber().orElse("unknown");
        globalTemplateContext.put("version", version);
        final String buildTime = Version.getInstance().getBuildTime().toString();
        globalTemplateContext.put("buildTime", buildTime);

        final boolean useFileLoader = config != null
                && config.containsKey("use-file-loader")
                && Boolean.parseBoolean(config.get("use-file-loader").toString());

        final boolean cacheActive = config != null
                && config.containsKey("cache-active")
                && Boolean.parseBoolean(config.get("cache-active").toString());

        final String templatesPath = config != null && config.containsKey("templates-path")
            ? config.get("templates-path").toString()
            : null;

        final Locale locale = resolveLocale(config);

        this.templateProcessor = new PebbleTemplateProcessor(globalTemplateContext, useFileLoader, cacheActive,
                locale, templatesPath);

        LOGGER.info(
                "Pebble template processor initialized (fileLoader: {}, cacheActive: {}, templatesPath: {}, locale: {})",
                useFileLoader, cacheActive, templatesPath != null
                    ? templatesPath
                    : "default",
                locale);
    }

    /**
     * Resolves the locale from the configuration.
     * 
     * @param config the configuration map
     * @return the resolved Locale
     */
    private static Locale resolveLocale(final Map<String, Object> config) {
        if (config == null) {
            return Locale.getDefault();
        }
        final Object localeObj = config.get("locale");
        if (localeObj == null) {
            return Locale.getDefault();
        }
        final String localeStr = localeObj.toString().trim();
        if (localeStr.isEmpty()) {
            return Locale.getDefault();
        }
        return Locale.forLanguageTag(localeStr);
    }

    /**
     * Returns the PebbleTemplateProcessor instance.
     *
     * @param caller the plugin record requesting the processor
     * @return the TemplateProcessor instance
     */
    @Override
    public TemplateProcessor get(final PluginRecord<?> caller) {
        return this.templateProcessor;
    }
}
