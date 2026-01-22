package org.facet.html;

import static org.facet.html.internal.HtmlResponseHelper.acceptsHtml;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NonNull;
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
 * </pre>
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

    @Inject("config")
    private Map<String, Object> config;

    private String loginUri;
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
    public void handle(final @NonNull ServiceRequest<?> request, final ServiceResponse<?> response) throws Exception {
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

        // Redirect to login page with 302 Found
        LOGGER.debug("Redirecting to login page: {} (original path: {})", loginUri, path);
        response.getExchange().getResponseHeaders().remove(Headers.WWW_AUTHENTICATE);
        response.getExchange().setStatusCode(StatusCodes.FOUND);
        response.getExchange().getResponseHeaders().put(Headers.LOCATION, loginUri + "?redirect=" + path);

        // Clear expired/invalid auth cookie to prevent infinite redirect loop
        // This ensures the browser doesn't keep sending an expired JWT token
        response.getExchange().getResponseHeaders().put(Headers.SET_COOKIE,
            "rh_auth=; Max-Age=0; Path=/; HttpOnly; SameSite=Lax");

        // Set content type to prevent any body processing
        response.setContentTypeAsJson();
    }

    @Override
    public boolean resolve(final @NonNull ServiceRequest<?> request, final ServiceResponse<?> response) {
        // Only intercept browser requests (those accepting HTML)
        return acceptsHtml(request.getHeaders());
    }
}
