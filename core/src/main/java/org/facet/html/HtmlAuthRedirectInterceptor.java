package org.facet.html;

import static org.facet.html.internal.HtmlResponseHelper.acceptsHtml;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.facet.html.internal.HtmxRequestDetector;
import org.restheart.exchange.ServiceRequest;
import org.restheart.exchange.ServiceResponse;
import org.restheart.plugins.Inject;
import org.restheart.plugins.InterceptPoint;
import org.restheart.plugins.OnInit;
import org.restheart.plugins.RegisterPlugin;
import org.restheart.plugins.WildcardInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;

/**
 * Generic interceptor that redirects browser requests to a login page when authentication fails.
 *
 * <p>
 * This interceptor executes at the REQUEST_AFTER_FAILED_AUTH intercept point, which runs
 * after authentication/authorization fails but before the error response is sent to the client.
 * It provides a better user experience for browser-based applications by redirecting to a
 * login page instead of showing raw 401 errors.
 * </p>
 *
 * <h3>Configuration</h3>
 * 
 * <pre>
 * /html-auth-redirect-interceptor:
 *   enabled: true
 *   login-uri: /login  # URI to redirect to on authentication failure
 *   # Optional: paths that should not be redirected (e.g., API endpoints)
 *   exclude-paths: [/api, /tokens]
 *   redirect-param: redirect   # Optional: query parameter carrying the original URL (default: "redirect")
 *   cookie-name: rh_auth       # Optional: auth cookie to clear on failure (default: "rh_auth")
 * </pre>
 *
 * <p>Only unauthenticated requests are redirected: an authenticated user that is merely not
 * authorized (403) keeps the error response and the auth cookie. The original URL (raw path and
 * query string) is URL-encoded into the redirect parameter. HTMX requests get the login URL in an
 * {@code HX-Redirect} header instead of a 302, so that the browser navigates to the login page
 * rather than swapping it into the fragment target; for them the page URL from
 * {@code HX-Current-URL} is used as redirect target, since the request URL is a fragment or an
 * action endpoint.
 *
 * <p>
 * <strong>Use Cases:</strong>
 * </p>
 * <ul>
 * <li>RESTHeart Browser - redirect to login page</li>
 * <li>Custom HTML interfaces requiring authentication</li>
 * <li>Any web application mixing API and browser access</li>
 * </ul>
 *
 * <p>
 * <strong>Note:</strong> This interceptor only affects HTML-capable requests
 * (either {@code Accept: text/html} or {@code HX-Request: true}).
 * API clients (e.g., {@code Accept: application/json}) still receive standard 401 responses.
 * </p>
 */
@RegisterPlugin(
    name = "html-auth-redirect-interceptor",
    description = "Redirects browser requests to login page on authentication failure",
    interceptPoint = InterceptPoint.REQUEST_AFTER_FAILED_AUTH,
    priority = 1000,
    enabledByDefault = false)
public class HtmlAuthRedirectInterceptor implements WildcardInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlAuthRedirectInterceptor.class);

    /** Default name of the RESTHeart auth cookie (see authCookieSetter). */
    public static final String DEFAULT_COOKIE_NAME = "rh_auth";

    private static final String DEFAULT_REDIRECT_PARAM = "redirect";

    private static final HttpString HX_REDIRECT = new HttpString("HX-Redirect");

    @Inject("config")
    private Map<String, Object> config;

    private String loginUri;
    private String redirectParam;
    private String cookieName;
    private String[] excludePaths;

    /**
     * No-arg constructor for plugin registration via @RegisterPlugin annotation.
     */
    public HtmlAuthRedirectInterceptor() {
        // Empty - config will be injected via @Inject and onInit() called by framework
    }

    /**
     * Constructor for programmatic registration with explicit configuration.
     * Used when another plugin (e.g., BrowserService) dynamically registers this interceptor.
     *
     * @param config the configuration map containing login-uri and optional exclude-paths
     */
    public HtmlAuthRedirectInterceptor(final Map<String, Object> config) {
        this.config = config;
        onInit();
    }

    @OnInit
    public void onInit() {
        if (config == null || !config.containsKey("login-uri")) {
            throw new IllegalStateException(
                    "html-auth-redirect-interceptor requires mandatory 'login-uri' configuration");
        }
        this.loginUri = config.get("login-uri").toString();
        this.redirectParam = config.getOrDefault("redirect-param", DEFAULT_REDIRECT_PARAM).toString();
        this.cookieName = config.getOrDefault("cookie-name", DEFAULT_COOKIE_NAME).toString();

        // Optional: paths to exclude from redirection (e.g., API endpoints)
        if (config.containsKey("exclude-paths")) {
            final Object excludePathsObj = config.get("exclude-paths");
            if (excludePathsObj instanceof String[] stringArray) {
                this.excludePaths = stringArray;
            } else if (excludePathsObj instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                final var excludedPaths = (List<String>) list;
                this.excludePaths = excludedPaths.toArray(new String[0]);
            }
        }

        LOGGER.info("HTML auth redirect interceptor initialized with: login-uri={}, exclude-paths={}",
                loginUri, excludePaths != null
                    ? String.join(", ", excludePaths)
                    : "none");
    }

    @Override
    public void handle(final ServiceRequest<?> request, final ServiceResponse<?> response) throws Exception {
        final String path = request.getPath();

        // Check if path should be excluded from redirection
        if (excludePaths != null) {
            for (final String excludePath : excludePaths) {
                if (path.startsWith(excludePath)) {
                    LOGGER.debug("Skipping redirect for excluded path: {}", path);
                    return;
                }
            }
        }

        final boolean htmx = HtmxRequestDetector.isHtmxRequest(request);
        final var responseHeaders = response.getExchange().getResponseHeaders();

        // Use the raw (still encoded) request URI, so that encoded characters in document ids
        // survive the round trip through the redirect parameter
        String[] original = {request.getExchange().getRequestURI(), request.getQueryString()};
        if (htmx) {
            // The HTMX request targets a fragment or an action endpoint: send the user back to
            // the page they were on instead
            final String[] current = localPathOf(HtmxRequestDetector.getHxCurrentUrl(request));
            if (current != null) {
                original = current;
            }
        }
        final String loginUrl = buildLoginRedirectUrl(loginUri, redirectParam, original[0], original[1]);

        responseHeaders.remove(Headers.WWW_AUTHENTICATE);

        if (htmx) {
            // HTMX follows a 302 transparently and would swap the login page into the target
            // element; HX-Redirect makes the browser navigate instead. The status stays 401.
            LOGGER.debug("HTMX request: sending HX-Redirect to {} (original path: {})", loginUrl, path);
            responseHeaders.put(HX_REDIRECT, loginUrl);
        } else {
            // Redirect to login page with 302 Found
            LOGGER.debug("Redirecting to login page: {} (original path: {})", loginUrl, path);
            response.getExchange().setStatusCode(StatusCodes.FOUND);
            responseHeaders.put(Headers.LOCATION, loginUrl);
        }

        // Clear expired/invalid auth cookie to prevent infinite redirect loop
        // This ensures the browser doesn't keep sending an expired JWT token
        responseHeaders.put(Headers.SET_COOKIE,
            cookieName + "=; Max-Age=0; Path=/; HttpOnly; SameSite=Lax");

        // Set content type to prevent any body processing
        response.setContentTypeAsJson();
    }

    /**
     * Builds the login page URL carrying the original request as a URL-encoded query parameter.
     *
     * @param loginUri the login page URI (e.g. {@code /login})
     * @param redirectParam the query parameter name (e.g. {@code redirect})
     * @param path the original request path
     * @param queryString the original query string, may be null or empty
     * @return e.g. {@code /login?redirect=%2Fmydb%2Fproducts%3Fpage%3D2}
     */
    static String buildLoginRedirectUrl(
            final String loginUri,
            final String redirectParam,
            final String path,
            final String queryString) {
        final String original = queryString == null || queryString.isEmpty()
            ? path
            : path + "?" + queryString;
        final String separator = loginUri.contains("?") ? "&" : "?";
        return loginUri + separator + redirectParam + "=" + URLEncoder.encode(original, StandardCharsets.UTF_8);
    }

    /**
     * Extracts the raw path and query of a URL, e.g. the {@code HX-Current-URL} header.
     *
     * <p>Scheme and host are discarded, so the result is always a local target.
     *
     * @param url an absolute or relative URL, may be null
     * @return {@code {rawPath, rawQuery}} (query may be null), or null when the URL is unusable
     */
    static String[] localPathOf(final String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            final URI uri = new URI(url.trim());
            final String rawPath = uri.getRawPath();
            if (rawPath == null || !rawPath.startsWith("/")) {
                return null;
            }
            return new String[] {rawPath, uri.getRawQuery()};
        } catch (final URISyntaxException e) {
            LOGGER.debug("Ignoring unparsable HX-Current-URL '{}': {}", url, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean resolve(final ServiceRequest<?> request, final ServiceResponse<?> response) {
        // Only unauthenticated browser requests (those accepting HTML). This intercept point also
        // runs for authenticated users that are not authorized (403): they must not be redirected,
        // nor have their auth cookie cleared.
        return !request.isAuthenticated() && acceptsHtml(request.getHeaders());
    }
}
