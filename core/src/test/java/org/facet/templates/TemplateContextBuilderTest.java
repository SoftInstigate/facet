package org.facet.templates;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.restheart.exchange.ExchangeKeys.METHOD;
import org.restheart.exchange.Request;

import io.undertow.security.idm.Account;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link TemplateContextBuilder}.
 */
class TemplateContextBuilderTest {

    // ──────────────────────────────────────────────────────────────────────
    // create()
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create() with null global context produces empty context")
    void createWithNullGlobalContext() {
        final var builder = TemplateContextBuilder.create(null);
        final var ctx = builder.build();
        assertTrue(ctx.isEmpty());
    }

    @Test
    @DisplayName("create() merges global context into builder")
    void createMergesGlobalContext() {
        final Map<String, Object> global = new HashMap<>();
        global.put("version", "1.0.0");
        global.put("env", "test");
        final var ctx = TemplateContextBuilder.create(global).build();

        assertEquals("1.0.0", ctx.get("version"));
        assertEquals("test", ctx.get("env"));
    }

    // ──────────────────────────────────────────────────────────────────────
    // withAuthenticatedUser()
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("withAuthenticatedUser")
    class WithAuthenticatedUser {

        @Test
        @DisplayName("Sets isAuthenticated=false when no account")
        void unauthenticatedUser() {
            Request<?> request = mock(Request.class);
            when(request.getAuthenticatedAccount()).thenReturn(null);
            when(request.getMethod()).thenReturn(METHOD.GET);

            final var ctx = TemplateContextBuilder.create(null)
                    .withAuthenticatedUser(request)
                    .build();

            assertFalse((Boolean) ctx.get("isAuthenticated"));
            assertNull(ctx.get("username"));
            assertNull(ctx.get("roles"));
        }

        @Test
        @DisplayName("Sets isAuthenticated=true, username, roles when authenticated")
        void authenticatedUser() {
            Request<?> request = mock(Request.class);

            Account account = mock(Account.class);
            Principal principal = mock(Principal.class);
            when(principal.getName()).thenReturn("alice");
            when(account.getPrincipal()).thenReturn(principal);
            when(account.getRoles()).thenReturn(Set.of("admin", "user"));
            when(request.getAuthenticatedAccount()).thenReturn(account);
            when(request.getMethod()).thenReturn(METHOD.GET);

            final var ctx = TemplateContextBuilder.create(null)
                    .withAuthenticatedUser(request)
                    .build();

            assertTrue((Boolean) ctx.get("isAuthenticated"));
            assertEquals("alice", ctx.get("username"));
            Set<?> roles = (Set<?>) ctx.get("roles");
            assertTrue(roles.contains("admin"));
            assertTrue(roles.contains("user"));
        }

        @Test
        @DisplayName("Sets requestMethod=POST for POST requests")
        void setsRequestMethodPost() {
            Request<?> request = mock(Request.class);
            when(request.getAuthenticatedAccount()).thenReturn(null);
            when(request.getMethod()).thenReturn(METHOD.POST);

            final var ctx = TemplateContextBuilder.create(null)
                    .withAuthenticatedUser(request)
                    .build();

            assertEquals("POST", ctx.get("requestMethod"));
        }

        @Test
        @DisplayName("Sets requestMethod=GET for GET requests")
        void requestMethodGet() {
            Request<?> request = mock(Request.class);
            when(request.getAuthenticatedAccount()).thenReturn(null);
            when(request.getMethod()).thenReturn(METHOD.GET);

            final var ctx = TemplateContextBuilder.create(null)
                    .withAuthenticatedUser(request)
                    .build();

            assertEquals("GET", ctx.get("requestMethod"));
        }

        @Test
        @DisplayName("Sets requestMethod=PATCH for PATCH requests")
        void requestMethodPatch() {
            Request<?> request = mock(Request.class);
            when(request.getAuthenticatedAccount()).thenReturn(null);
            when(request.getMethod()).thenReturn(METHOD.PATCH);

            final var ctx = TemplateContextBuilder.create(null)
                    .withAuthenticatedUser(request)
                    .build();

            assertEquals("PATCH", ctx.get("requestMethod"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // with() and withServiceData()
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Context merging")
    class ContextMerging {

        @Test
        @DisplayName("with() adds key-value pair")
        void withAddsKeyValue() {
            final var ctx = TemplateContextBuilder.create(null)
                    .with("page", 2)
                    .build();

            assertEquals(2, ctx.get("page"));
        }

        @Test
        @DisplayName("withServiceData() merges all keys")
        void withServiceDataMergesAll() {
            final Map<String, Object> serviceData = new HashMap<>();
            serviceData.put("totalItems", 42L);
            serviceData.put("coll", "products");

            final var ctx = TemplateContextBuilder.create(null)
                    .withServiceData(serviceData)
                    .build();

            assertEquals(42L, ctx.get("totalItems"));
            assertEquals("products", ctx.get("coll"));
        }

        @Test
        @DisplayName("withServiceData() with null is no-op")
        void withServiceDataNullIsNoOp() {
            final Map<String, Object> global = new HashMap<>();
            global.put("key", "val");

            final var ctx = TemplateContextBuilder.create(global)
                    .withServiceData(null)
                    .build();

            assertEquals("val", ctx.get("key"));
        }

        @Test
        @DisplayName("later with() calls override earlier values")
        void laterCallsOverrideEarlier() {
            final Map<String, Object> global = new HashMap<>();
            global.put("key", "old");

            final var ctx = TemplateContextBuilder.create(global)
                    .with("key", "new")
                    .build();

            assertEquals("new", ctx.get("key"));
        }

        @Test
        @DisplayName("build() returns independent copy (mutation safe)")
        void buildReturnsCopy() {
            final var builder = TemplateContextBuilder.create(null);
            builder.with("k", "v");
            final var ctx1 = builder.build();
            final var ctx2 = builder.build();
            ctx1.put("extra", "x");
            assertNull(ctx2.get("extra"));
        }
    }
}
