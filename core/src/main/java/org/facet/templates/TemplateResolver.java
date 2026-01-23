package org.facet.templates;

import java.util.Optional;

import org.restheart.exchange.ExchangeKeys.TYPE;

/**
 * Resolves template names based on request paths using hierarchical fallback.
 *
 * <p>This interface defines the contract for template resolution strategies
 * in the Facet SSR framework. Implementations determine which template
 * file should be used to render a given resource path.
 *
 * <p><strong>Recommended Convention:</strong> Use explicit action templates for clean separation:
 * <ul>
 * <li><strong>list.html</strong> - For collection views (multiple items)</li>
 * <li><strong>view.html</strong> - For document views (single item)</li>
 * <li><strong>index.html</strong> - Optional unified fallback (when list/view can share logic)</li>
 * </ul>
 *
 * <p>Example resolution for path "/products" (collection):
 * <pre>
 * 1. templates/products/list.html      (recommended - explicit collection view)
 * 2. templates/products/index.html     (optional unified fallback)
 * 3. templates/list.html               (global collection template)
 * 4. templates/index.html              (global fallback)
 * </pre>
 *
 * <p>Example resolution for path "/products/:id" (document):
 * <pre>
 * 1. templates/products/:id/view.html  (document-specific override)
 * 2. templates/products/view.html      (recommended - explicit document view)
 * 3. templates/products/index.html     (optional unified fallback)
 * 4. templates/view.html               (global document template)
 * 5. templates/index.html              (global fallback)
 * </pre>
 *
 * @since 1.0
 */
public interface TemplateResolver {

    /**
     * Resolves a template name for the given request path.
     *
     * <p>This method only checks for index.html templates.
     * For explicit action-aware resolution (list.html, view.html), use {@link #resolve(TemplateProcessor, String, TYPE)}.
     *
     * <p>The resolver searches for templates in a hierarchical manner,
     * starting with the most specific path and falling back to parent paths
     * until a template is found.
     *
     * @param templateProcessor the template processor to check for template existence
     * @param requestPath the request path (e.g., "/restheart/users")
     * @return the resolved template name (e.g., "restheart/users/index.html"),
     *         or empty Optional if no template is found
     */
    Optional<String> resolve(TemplateProcessor templateProcessor, String requestPath);

    /**
     * Resolves a template name for the given request path with explicit action-aware resolution.
     *
     * <p><strong>Recommended Pattern:</strong> Use explicit templates for clarity:
     * <ul>
     * <li><strong>list.html</strong> - Collection views (no conditional logic needed)</li>
     * <li><strong>view.html</strong> - Document views (no conditional logic needed)</li>
     * <li><strong>index.html</strong> - Optional fallback (for simple cases)</li>
     * </ul>
     *
     * <p><strong>Resolution Strategy:</strong></p>
     * <ul>
     * <li>For COLLECTION requests: tries list.html first, then falls back to index.html</li>
     * <li>For DOCUMENT requests: tries view.html first, then falls back to index.html</li>
     * <li>For other types: uses index.html only</li>
     * </ul>
     *
     * <p><strong>Hierarchical Fallback:</strong></p>
     * <p>Each template name is searched hierarchically from most specific to least specific path.</p>
     *
     * @param templateProcessor the template processor to check for template existence
     * @param requestPath the request path (e.g., "/products" or "/products/123")
     * @param requestType the type of request (ROOT, DB, COLLECTION, DOCUMENT, etc.)
     * @return the resolved template name if found, or empty Optional if no template is found
     */
    Optional<String> resolve(TemplateProcessor templateProcessor, String requestPath, TYPE requestType);

    /**
     * Resolves a fragment template for HTMX requests based on the hx-target element ID.
     *
     * <p>Fragments are organized in {@code _fragments/} subdirectories for better code organization.
     * The target ID from the {@code HX-Target} header is used as the template filename.</p>
     *
     * <p>For an HTMX request to {@code /mydb/mycoll} with {@code HX-Target: #product-details},
     * this will look for {@code templates/mydb/mycoll/_fragments/product-details.html} or fall back
     * to {@code templates/_fragments/product-details.html}.</p>
     *
     * @param templateProcessor the template processor to check for template existence
     * @param requestPath the request path (e.g., "/mydb/mycoll")
     * @param targetId the hx-target element ID without '#' prefix (e.g., "product-details")
     * @return the resolved fragment template name if found, empty Optional otherwise
     */
    Optional<String> resolveFragment(TemplateProcessor templateProcessor, String requestPath, String targetId);
}
