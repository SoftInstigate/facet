package org.facet.html;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link LoginService#safeRedirect(String, String)}.
 */
class LoginServiceTest {

    private static final String FALLBACK = "/";

    @Test
    @DisplayName("null returns fallback")
    void nullReturnsFallback() {
        assertEquals(FALLBACK, LoginService.safeRedirect(null, FALLBACK));
    }

    @Test
    @DisplayName("blank returns fallback")
    void blankReturnsFallback() {
        assertEquals(FALLBACK, LoginService.safeRedirect("", FALLBACK));
        assertEquals(FALLBACK, LoginService.safeRedirect("   ", FALLBACK));
    }

    @Test
    @DisplayName("local path with query is returned unchanged")
    void localPathAccepted() {
        assertEquals("/mydb/products?page=2", LoginService.safeRedirect("/mydb/products?page=2", FALLBACK));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://evil.example",
        "//evil.example",
        "/\\evil.example",
        "javascript:alert(1)",
        "/ok\r\nSet-Cookie: x",
        "/1;alert(1)//\\",
        "/a'b",
        "/a\"b",
        "/a<b>",
        "/a`b",
        "/a b",
        "/\\evil.example"
    })
    @DisplayName("unsafe targets return fallback")
    void unsafeRejected(String requested) {
        assertEquals(FALLBACK, LoginService.safeRedirect(requested, FALLBACK));
    }
}
