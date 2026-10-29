package org.facet.html;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link HtmlAuthRedirectInterceptor#buildLoginRedirectUrl}.
 */
class HtmlAuthRedirectInterceptorTest {

    @Test
    @DisplayName("path only")
    void pathOnly() {
        assertEquals("/login?redirect=%2Fmydb%2Fproducts",
                HtmlAuthRedirectInterceptor.buildLoginRedirectUrl("/login", "redirect", "/mydb/products", null));
    }

    @Test
    @DisplayName("path and query are encoded once as a whole")
    void pathAndQuery() {
        String query = "page=2&filter={\"a\":1}";
        String expected = "/login?redirect="
                + URLEncoder.encode("/mydb/products?" + query, StandardCharsets.UTF_8);
        String actual = HtmlAuthRedirectInterceptor.buildLoginRedirectUrl("/login", "redirect", "/mydb/products", query);
        assertEquals(expected, actual);
        assertFalse(actual.contains("%25"), "must not be double encoded");
        assertTrue(actual.contains("%3Fpage%3D2%26filter"));
    }

    @Test
    @DisplayName("null and empty query behave the same")
    void nullAndEmptyQuery() {
        assertEquals(
                HtmlAuthRedirectInterceptor.buildLoginRedirectUrl("/login", "redirect", "/a", null),
                HtmlAuthRedirectInterceptor.buildLoginRedirectUrl("/login", "redirect", "/a", ""));
    }

    @Test
    @DisplayName("loginUri already containing ? uses &")
    void loginUriWithQuery() {
        assertEquals("/login?lang=en&redirect=%2Fa",
                HtmlAuthRedirectInterceptor.buildLoginRedirectUrl("/login?lang=en", "redirect", "/a", null));
    }

    @Test
    @DisplayName("localPathOf keeps raw path and query of an absolute URL, drops scheme and host")
    void localPathOfAbsolute() {
        String[] r = HtmlAuthRedirectInterceptor.localPathOf("http://localhost:8080/mydb/products?page=2&q=a%20b");
        assertArrayEquals(new String[] {"/mydb/products", "page=2&q=a%20b"}, r);
    }

    @Test
    @DisplayName("localPathOf accepts a relative URL without query")
    void localPathOfRelative() {
        assertArrayEquals(new String[] {"/x", null}, HtmlAuthRedirectInterceptor.localPathOf("/x"));
    }

    @Test
    @DisplayName("localPathOf returns null for blank, unparsable or non-path URLs")
    void localPathOfInvalid() {
        assertNull(HtmlAuthRedirectInterceptor.localPathOf(null));
        assertNull(HtmlAuthRedirectInterceptor.localPathOf("  "));
        assertNull(HtmlAuthRedirectInterceptor.localPathOf("http://[bad"));
        assertNull(HtmlAuthRedirectInterceptor.localPathOf("mailto:a@b.c"));
    }

    @Test
    @DisplayName("encodePath re-encodes reserved and non-ASCII characters, keeps slashes")
    void encodePath() {
        assertEquals("/mydb/products/a%3Fb", HtmlAuthRedirectInterceptor.encodePath("/mydb/products/a?b"));
        assertEquals("/a%20b/c%25d%23e", HtmlAuthRedirectInterceptor.encodePath("/a b/c%d#e"));
        assertEquals("/caff%C3%A8", HtmlAuthRedirectInterceptor.encodePath("/caffè"));
        assertEquals("/plain/path", HtmlAuthRedirectInterceptor.encodePath("/plain/path"));
        assertEquals("/", HtmlAuthRedirectInterceptor.encodePath(""));
    }

    @Test
    @DisplayName("encoded id round-trips through the redirect parameter")
    void encodedIdRoundTrip() {
        String url = HtmlAuthRedirectInterceptor.buildLoginRedirectUrl("/login", "redirect",
                HtmlAuthRedirectInterceptor.encodePath("/mydb/products/a?b"), null);
        assertEquals("/login?redirect=%2Fmydb%2Fproducts%2Fa%253Fb", url);
    }

    @Test
    @DisplayName("custom parameter name is used")
    void customParam() {
        assertEquals("/login?next=%2Fa",
                HtmlAuthRedirectInterceptor.buildLoginRedirectUrl("/login", "next", "/a", null));
    }
}
