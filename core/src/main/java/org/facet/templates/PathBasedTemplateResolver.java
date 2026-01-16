package org.facet.templates;

import java.util.Arrays;
import java.util.Optional;

import org.restheart.exchange.ExchangeKeys.TYPE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Path-based template resolver with hierarchical fallback.
 *
 * <p>
 * This resolver maps request paths to template files using a convention-over-configuration
 * approach. Templates are organized in a directory structure that mirrors the URL structure,
 * with each resource having an optional {@code index.html} template.
 *
 * <p>
 * <strong>Full Page Resolution Algorithm:</strong>
 * </p>
 *
 * <pre>
 * Request: GET /a/b/c
 *
 * Search order:
 * 1. templates/a/b/c/index.html    (most specific - exact match)
 * 2. templates/a/b/index.html      (parent level)
 * 3. templates/a/index.html        (top level)
 * 4. templates/index.html          (root fallback - catch-all)
 * 5. return empty Optional         (no template found)
 * </pre>
 *
 * <p>
 * <strong>Fragment Resolution Algorithm (HTMX):</strong>
 * </p>
 *
 * <pre>
 * HTMX Request: GET /a/b/c with HX-Target: #my-target
 *
 * Search order:
 * 1. templates/a/b/c/_fragments/my-target.html    (resource-specific fragment)
 * 2. templates/_fragments/my-target.html          (root fallback fragment)
 * 3. return empty Optional                        (no fragment found - strict mode error)
 *
 * Note: Fragments are organized in _fragments/ subdirectories for better code organization.
 * No hierarchical walk for fragments - only exact path or root fallback.
 * </pre>
 *
 * <p>
 * <strong>Example Usage:</strong>
 * </p>
 *
 * <pre>
 * // Full page request: GET /mydb/mycollection
 * // Try templates/mydb/mycollection/index.html (doesn't exist)
 * // Try templates/mydb/index.html (doesn't exist)
 * // Try templates/index.html (exists - MongoDB browser UI) → use it
 *
 * // HTMX fragment request: GET /mydb/mycollection with HX-Target: #product-list
 * // Try templates/mydb/mycollection/_fragments/product-list.html (doesn't exist)
 * // Try templates/_fragments/product-list.html (exists) → use it
 * </pre>
 *
 * <p>
 * This is a stateless utility class - the template processor is passed to the resolve() method
 * rather than being injected as a dependency. This eliminates coupling and allows html package
 * to use templates package without requiring dependency injection.
 * </p>
 *
 * @since 1.0
 */
public class PathBasedTemplateResolver implements TemplateResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathBasedTemplateResolver.class);

    private static final String INDEX_TEMPLATE = "/index";

    /**
     * Creates a new PathBasedTemplateResolver.
     * This is a stateless utility class - template processor is passed to resolve() method.
     */
    public PathBasedTemplateResolver() {
        LOGGER.debug("PathBasedTemplateResolver initialized");
    }

    /**
     * Resolves a template name for the given request path using hierarchical fallback.
     *
     * <p>This method uses the legacy resolution strategy (index.html only).
     * For action-aware resolution, use {@link #resolve(TemplateProcessor, String, TYPE)}.
     *
     * @param templateProcessor the template processor to check for template existence
     * @param requestPath the request path (e.g., "/restheart/users")
     * @return the resolved template name (e.g., "restheart/users/index.html") if found, or empty Optional if not found
     */
    @Override
    public Optional<String> resolve(final TemplateProcessor templateProcessor, final String requestPath) {
        return resolveTemplate(templateProcessor, requestPath, INDEX_TEMPLATE, true, "index");
    }

    /**
     * Resolves a template name with action-aware resolution (list.html, view.html, index.html).
     *
     * <p><strong>Resolution Strategy:</strong></p>
     * <ul>
     * <li><strong>COLLECTION requests:</strong>
     *   <ol>
     *     <li>products/list.html (explicit - recommended)</li>
     *     <li>products/index.html (unified fallback)</li>
     *     <li>Hierarchical parent fallback</li>
     *   </ol>
     * </li>
     * <li><strong>DOCUMENT requests:</strong>
     *   <ol>
     *     <li>products/:id/view.html (specific override)</li>
     *     <li>products/:id/index.html (specific unified)</li>
     *     <li>products/view.html (explicit generic)</li>
     *     <li>products/index.html (unified fallback)</li>
     *     <li>Hierarchical parent fallback</li>
     *   </ol>
     * </li>
     * <li><strong>Other types:</strong> Uses index.html only</li>
     * </ul>
     *
     * @param templateProcessor the template processor to check for template existence
     * @param requestPath the request path (e.g., "/products/123")
     * @param requestType the type of request (ROOT, DB, COLLECTION, DOCUMENT, etc.)
     * @return the resolved template name if found, or empty Optional if not found
     */
    @Override
    public Optional<String> resolve(
            final TemplateProcessor templateProcessor,
            final String requestPath,
            final TYPE requestType) {

        if (requestType == null) {
            // Fallback to legacy resolution if type not provided
            return resolve(templateProcessor, requestPath);
        }

        LOGGER.debug("Resolving template for path '{}' with requestType: {}", requestPath, requestType);

        // Determine action template based on request type
        final String actionTemplate = getActionTemplate(requestType);

        if (actionTemplate != null) {
            // Try explicit action-based template first (list.html or view.html)
            final Optional<String> explicitTemplate = resolveTemplate(
                    templateProcessor,
                    requestPath,
                    "/" + actionTemplate,
                    true,
                    actionTemplate);

            if (explicitTemplate.isPresent()) {
                LOGGER.debug("Resolved explicit action template: {}", explicitTemplate.get());
                return explicitTemplate;
            }

            LOGGER.debug("No explicit action template found, falling back to index.html");
        }

        // Fallback to unified index.html template
        return resolve(templateProcessor, requestPath);
    }

    /**
     * Determines the action template name based on request type.
     *
     * <p>Mapping:</p>
     * <ul>
     * <li>COLLECTION, ROOT, DB → "list" (collection views, Pebble adds .html)</li>
     * <li>DOCUMENT → "view" (single resource view, Pebble adds .html)</li>
     * <li>Others → null (use index.html only)</li>
     * </ul>
     *
     * <p>Note: Template names should NOT include .html extension as Pebble adds it automatically.</p>
     *
     * @param requestType the MongoDB request type
     * @return the action template name (without path or .html extension), or null for index.html only
     */
    private String getActionTemplate(final TYPE requestType) {
        return switch (requestType) {
            case COLLECTION, ROOT, DB -> "list";
            case DOCUMENT -> "view";
            default -> null;
        };
    }

    /**
     * Resolves a fragment template for HTMX requests based on the hx-target element ID.
     *
     * <p>
     * Fragments are organized in {@code _fragments/} subdirectories for better code organization.
     * The target ID from the {@code HX-Target} header is used as the template filename.
     * </p>
     *
     * <p>
     * For an HTMX request to {@code /mydb/mycoll} with {@code HX-Target: #product-details},
     * this will look for:
     * <ul>
     * <li>{@code templates/mydb/mycoll/_fragments/product-details.html} (resource-specific)</li>
     * <li>{@code templates/_fragments/product-details.html} (root fallback)</li>
     * </ul>
     * </p>
     *
     * @param templateProcessor the template processor to check for template existence
     * @param requestPath the request path (e.g., "/mydb/mycoll")
     * @param targetId the hx-target element ID without '#' prefix (e.g., "product-details")
     * @return the resolved fragment template name if found
     * @throws TemplateNotFoundException if no fragment template is found (strict mode)
     */
    public Optional<String> resolveFragment(final TemplateProcessor templateProcessor,
            final String requestPath,
            final String targetId) {
        if (targetId == null || targetId.isBlank()) {
            LOGGER.debug("Empty target ID, cannot resolve fragment");
            return Optional.empty();
        }

        return resolveTemplate(templateProcessor, requestPath, "/_fragments/" + targetId, false, "_fragments/" + targetId);
    }

    /**
     * Unified template resolution method with configurable strategy.
     *
     * @param templateProcessor the template processor to check for template existence
     * @param requestPath the request path (e.g., "/mydb/mycoll")
     * @param templateSuffix the suffix to append to paths (e.g., "/index" or "/targetId")
     * @param walkHierarchy whether to walk up the path hierarchy (true for full pages, false for fragments)
     * @param rootFallback the template name to use as root fallback (e.g., "index" or "targetId")
     * @return the resolved template name if found, empty Optional otherwise
     */
    private Optional<String> resolveTemplate(
            final TemplateProcessor templateProcessor,
            final String requestPath,
            final String templateSuffix,
            final boolean walkHierarchy,
            final String rootFallback) {
        if (requestPath == null || requestPath.isBlank()) {
            LOGGER.debug("Empty request path, no template found");
            return Optional.empty();
        }

        final String normalizedPath = normalizePath(requestPath);

        if (normalizedPath.isBlank()) {
            // For root path, check for root fallback template
            final var template = tryTemplate(templateProcessor, rootFallback);
            if (template.isPresent()) {
                LOGGER.trace("Resolved template for root path: {}", template.get());
                return template;
            }
            LOGGER.debug("No template found for root path");
            return Optional.empty();
        }

        // 1. Try exact path with suffix
        var template = tryTemplate(templateProcessor, normalizedPath + templateSuffix);
        if (template.isPresent()) {
            LOGGER.trace("Resolved template for path '{}': {}", requestPath, template.get());
            return template;
        }

        // 2. Optionally walk up the path hierarchy (only for full page templates)
        if (walkHierarchy) {
            final String[] segments = normalizedPath.split("/");
            for (int i = segments.length - 1; i > 0; i--) {
                final String parentPath = String.join("/", Arrays.copyOf(segments, i));
                template = tryTemplate(templateProcessor, parentPath + templateSuffix);
                if (template.isPresent()) {
                    LOGGER.trace("Resolved template for path '{}' using parent fallback: {}", requestPath,
                            template.get());
                    return template;
                }
            }
        }

        // 3. Try root fallback as final option
        template = tryTemplate(templateProcessor, rootFallback);
        if (template.isPresent()) {
            LOGGER.trace("Resolved template for path '{}' using root fallback: {}", requestPath, rootFallback);
            return template;
        }

        // 4. No template found
        if (walkHierarchy) {
            LOGGER.debug("No template found for path '{}'", requestPath);
        } else {
            LOGGER.warn("No fragment template found for path '{}' and target '{}'", requestPath, rootFallback);
        }
        return Optional.empty();
    }

    /**
     * Normalizes a request path by removing leading and trailing slashes.
     *
     * @param path the request path to normalize (e.g., "/mydb/mycoll/")
     * @return the normalized path (e.g., "mydb/mycoll"), or empty string for root path
     */
    private String normalizePath(final String path) {
        if (path == null || path.isBlank()) {
            return "";
        }

        // Remove leading slash if present
        String normalized = path.startsWith("/")
            ? path.substring(1)
            : path;

        // Remove trailing slash if present
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    /**
     * Attempts to find a template at the given path.
     *
     * @param templateProcessor the template processor to check for template existence
     * @param templatePath the template path relative to templates directory (e.g., "restheart/users/index")
     * @return the template path if it exists, empty Optional otherwise
     */
    private Optional<String> tryTemplate(final TemplateProcessor templateProcessor, final String templatePath) {
        if (templateProcessor == null) {
            LOGGER.error("TemplateProcessor is null in tryTemplate()!");
            return Optional.empty();
        }

        LOGGER.trace("Checking if template exists: '{}'", templatePath);
        try {
            if (templateProcessor.templateExists(templatePath)) {
                LOGGER.info("✓ Found template: '{}'", templatePath);
                return Optional.of(templatePath);
            } else {
                LOGGER.debug("✗ Template does not exist: '{}'", templatePath);
            }
        } catch (final Exception e) {
            LOGGER.debug("Exception checking template '{}': {}", templatePath, e.getMessage());
        }

        return Optional.empty();
    }
}
