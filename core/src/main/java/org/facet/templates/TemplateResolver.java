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
 * <p>Example resolution for path "/products" (collection):
 * <pre>
 * 1. templates/products/list.html      (explicit - recommended)
 * 2. templates/products/index.html     (unified fallback)
 * 3. templates/list.html               (global explicit)
 * 4. templates/index.html              (global fallback)
 * </pre>
 *
 * <p>Example resolution for path "/products/:id" (document):
 * <pre>
 * 1. templates/products/:id/view.html  (specific override)
 * 2. templates/products/view.html      (explicit - recommended)
 * 3. templates/products/index.html     (unified fallback)
 * 4. templates/view.html               (global explicit)
 * 5. templates/index.html              (global fallback)
 * </pre>
 *
 * @since 1.0
 */
public interface TemplateResolver {

    /**
     * Resolves a template name for the given request path.
     *
     * <p>This method uses the legacy resolution strategy that only checks for index.html.
     * For action-aware resolution (list.html, view.html), use {@link #resolve(TemplateProcessor, String, TYPE)}.
     *
     * <p>The resolver should search for templates in a hierarchical manner,
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
     * Resolves a template name for the given request path with action-aware resolution.
     *
     * <p>This method supports both explicit action-based templates (list.html, view.html)
     * and unified templates (index.html) as fallback.
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
