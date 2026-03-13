package org.facet.templates;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.restheart.exchange.ExchangeKeys.TYPE;

/**
 * Tests for {@link PathBasedTemplateResolver} covering all branches of the
 * resolution algorithm: collection, document, fragment lookups, hierarchical
 * fallback, and strict fragment mode.
 */
class PathBasedTemplateResolverTest {

    private PathBasedTemplateResolver resolver;
    private TemplateProcessor tp;

    @BeforeEach
    void setUp() {
        resolver = new PathBasedTemplateResolver();
        tp = mock(TemplateProcessor.class);
        // Default: no template exists
        when(tp.templateExists(anyString())).thenReturn(false);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Collection resolution
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Collection request resolution")
    class CollectionResolution {

        @Test
        @DisplayName("Finds list.html at exact path")
        void findsListHtmlAtExactPath() {
            when(tp.templateExists("mydb/products/list")).thenReturn(true);

            Optional<String> result = resolver.resolve(tp, "/mydb/products", TYPE.COLLECTION);

            assertTrue(result.isPresent());
            assertEquals("mydb/products/list", result.get());
        }

        @Test
        @DisplayName("Falls back to index.html when list.html absent")
        void fallsBackToIndexWhenListAbsent() {
            when(tp.templateExists("mydb/products/index")).thenReturn(true);

            Optional<String> result = resolver.resolve(tp, "/mydb/products", TYPE.COLLECTION);

            assertTrue(result.isPresent());
            assertEquals("mydb/products/index", result.get());
        }

        @Test
        @DisplayName("Walks up hierarchy to parent list.html")
        void walksHierarchyToParentList() {
            when(tp.templateExists("mydb/list")).thenReturn(true);

            Optional<String> result = resolver.resolve(tp, "/mydb/products", TYPE.COLLECTION);

            assertTrue(result.isPresent());
            assertEquals("mydb/list", result.get());
        }

        @Test
        @DisplayName("Falls back to global list.html")
        void fallsBackToGlobalList() {
            when(tp.templateExists("list")).thenReturn(true);

            Optional<String> result = resolver.resolve(tp, "/mydb/products", TYPE.COLLECTION);

            assertTrue(result.isPresent());
            assertEquals("list", result.get());
        }

        @Test
        @DisplayName("Falls back to global index.html")
        void fallsBackToGlobalIndex() {
            when(tp.templateExists("index")).thenReturn(true);

            Optional<String> result = resolver.resolve(tp, "/mydb/products", TYPE.COLLECTION);

            assertTrue(result.isPresent());
            assertEquals("index", result.get());
        }

        @Test
        @DisplayName("Returns empty when no template found")
        void returnsEmptyWhenNoTemplate() {
            Optional<String> result = resolver.resolve(tp, "/mydb/products", TYPE.COLLECTION);

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Prefers list.html over index.html at same path")
        void prefersListOverIndex() {
            when(tp.templateExists("mydb/products/list")).thenReturn(true);
            when(tp.templateExists("mydb/products/index")).thenReturn(true);

            Optional<String> result = resolver.resolve(tp, "/mydb/products", TYPE.COLLECTION);

            assertEquals("mydb/products/list", result.get());
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Document resolution
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Document request resolution")
    class DocumentResolution {

        @Test
        @DisplayName("Finds document-specific view.html (deepest level)")
        void findsDocumentSpecificViewHtml() {
            when(tp.templateExists("mydb/products/123/view")).thenReturn(true);

            Optional<String> result = resolver.resolve(tp, "/mydb/products/123", TYPE.DOCUMENT);

            assertTrue(result.isPresent());
            assertEquals("mydb/products/123/view", result.get());
        }

        @Test
        @DisplayName("Falls back to collection-level view.html")
        void fallsBackToCollectionLevelView() {
            when(tp.templateExists("mydb/products/view")).thenReturn(true);

            Optional<String> result = resolver.resolve(tp, "/mydb/products/123", TYPE.DOCUMENT);

            assertTrue(result.isPresent());
            assertEquals("mydb/products/view", result.get());
        }

        @Test
        @DisplayName("Falls back to collection-level index.html")
        void fallsBackToCollectionLevelIndex() {
            when(tp.templateExists("mydb/products/index")).thenReturn(true);

            Optional<String> result = resolver.resolve(tp, "/mydb/products/123", TYPE.DOCUMENT);

            assertTrue(result.isPresent());
            assertEquals("mydb/products/index", result.get());
        }

        @Test
        @DisplayName("Falls back to global view.html")
        void fallsBackToGlobalView() {
            when(tp.templateExists("view")).thenReturn(true);

            Optional<String> result = resolver.resolve(tp, "/mydb/products/123", TYPE.DOCUMENT);

            assertTrue(result.isPresent());
            assertEquals("view", result.get());
        }

        @Test
        @DisplayName("Falls back to global index.html")
        void fallsBackToGlobalIndex() {
            when(tp.templateExists("index")).thenReturn(true);

            Optional<String> result = resolver.resolve(tp, "/mydb/products/123", TYPE.DOCUMENT);

            assertTrue(result.isPresent());
            assertEquals("index", result.get());
        }

        @Test
        @DisplayName("Returns empty when no template found")
        void returnsEmptyWhenNoTemplate() {
            Optional<String> result = resolver.resolve(tp, "/mydb/products/123", TYPE.DOCUMENT);

            assertFalse(result.isPresent());
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Fragment resolution (HTMX strict mode)
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Fragment resolution (HTMX)")
    class FragmentResolution {

        @Test
        @DisplayName("Finds resource-specific fragment")
        void findsResourceSpecificFragment() {
            when(tp.templateExists("mydb/products/_fragments/product-list")).thenReturn(true);

            Optional<String> result = resolver.resolveFragment(tp, "/mydb/products", "product-list");

            assertTrue(result.isPresent());
            assertEquals("mydb/products/_fragments/product-list", result.get());
        }

        @Test
        @DisplayName("Falls back to root fragment")
        void fallsBackToRootFragment() {
            when(tp.templateExists("_fragments/product-list")).thenReturn(true);

            Optional<String> result = resolver.resolveFragment(tp, "/mydb/products", "product-list");

            assertTrue(result.isPresent());
            assertEquals("_fragments/product-list", result.get());
        }

        @Test
        @DisplayName("Returns empty when no fragment found (strict mode)")
        void returnsEmptyWhenNoFragmentFound() {
            Optional<String> result = resolver.resolveFragment(tp, "/mydb/products", "product-list");

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Returns empty for null target ID")
        void returnsEmptyForNullTargetId() {
            Optional<String> result = resolver.resolveFragment(tp, "/mydb/products", null);

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Returns empty for blank target ID")
        void returnsEmptyForBlankTargetId() {
            Optional<String> result = resolver.resolveFragment(tp, "/mydb/products", "  ");

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Prefers resource-specific over root fragment")
        void prefersResourceSpecificOverRoot() {
            when(tp.templateExists("mydb/products/_fragments/form")).thenReturn(true);
            when(tp.templateExists("_fragments/form")).thenReturn(true);

            Optional<String> result = resolver.resolveFragment(tp, "/mydb/products", "form");

            assertEquals("mydb/products/_fragments/form", result.get());
        }

        @Test
        @DisplayName("Does NOT walk up hierarchy for fragments")
        void doesNotWalkHierarchyForFragments() {
            // Only the parent-level fragment exists — should NOT be found
            when(tp.templateExists("mydb/_fragments/product-list")).thenReturn(true);

            Optional<String> result = resolver.resolveFragment(tp, "/mydb/products", "product-list");

            assertFalse(result.isPresent());
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Error template resolution
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Error template resolution")
    class ErrorResolution {

        @Test
        @DisplayName("Finds status-specific error template")
        void findsStatusSpecificTemplate() {
            when(tp.templateExists("errors/404")).thenReturn(true);

            Optional<String> result = resolver.resolveError(tp, 404);

            assertTrue(result.isPresent());
            assertEquals("errors/404", result.get());
        }

        @Test
        @DisplayName("Falls back to generic error template")
        void fallsBackToGenericError() {
            when(tp.templateExists("error")).thenReturn(true);

            Optional<String> result = resolver.resolveError(tp, 500);

            assertTrue(result.isPresent());
            assertEquals("error", result.get());
        }

        @Test
        @DisplayName("Prefers specific over generic error template")
        void prefersSpecificOverGeneric() {
            when(tp.templateExists("errors/404")).thenReturn(true);
            when(tp.templateExists("error")).thenReturn(true);

            Optional<String> result = resolver.resolveError(tp, 404);

            assertEquals("errors/404", result.get());
        }

        @Test
        @DisplayName("Returns empty when no error template found")
        void returnsEmptyWhenNone() {
            Optional<String> result = resolver.resolveError(tp, 404);

            assertFalse(result.isPresent());
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Null and edge-case inputs
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Null request path returns empty")
        void nullPathReturnsEmpty() {
            Optional<String> result = resolver.resolve(tp, null, TYPE.COLLECTION);
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Blank request path returns empty")
        void blankPathReturnsEmpty() {
            Optional<String> result = resolver.resolve(tp, "  ", TYPE.COLLECTION);
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Null request type falls back to index-only resolution")
        void nullTypeFallsBackToIndexResolution() {
            when(tp.templateExists("mydb/products/index")).thenReturn(true);

            Optional<String> result = resolver.resolve(tp, "/mydb/products", null);

            assertTrue(result.isPresent());
            assertEquals("mydb/products/index", result.get());
        }

        @Test
        @DisplayName("Null template processor returns empty")
        void nullTemplateProcessorReturnsEmpty() {
            Optional<String> result = resolver.resolve(null, "/mydb/products", TYPE.COLLECTION);
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Trailing slash is normalized")
        void trailingSlashNormalized() {
            when(tp.templateExists("mydb/products/list")).thenReturn(true);

            Optional<String> result = resolver.resolve(tp, "/mydb/products/", TYPE.COLLECTION);

            assertTrue(result.isPresent());
            assertEquals("mydb/products/list", result.get());
        }
    }
}
