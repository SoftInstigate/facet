/**
 * Request Logger Interceptor
 *
 * A RESTHeart JavaScript interceptor that adds diagnostic response headers
 * for requests to /shop/ endpoints. Useful for debugging and demonstrating
 * the interceptor lifecycle.
 *
 * Demonstrates:
 *   - Writing a RESTHeart interceptor in JavaScript
 *   - Using the resolve() predicate to filter which requests are intercepted
 *   - Reading request data and writing response headers
 *   - RESPONSE intercept point (runs after the service handler)
 *
 * The headers added:
 *   X-Facet-Plugin: request-logger
 *   X-Request-Path: the request path
 *   X-Request-Method: GET, POST, etc.
 */

export const options = {
    name: "requestLogger",
    description: "Adds diagnostic headers to /shop/ responses",
    interceptPoint: "RESPONSE"
};

export function resolve(request) {
    return request.getPath().startsWith("/shop/");
}

export function handle(request, response) {
    const HttpString = Java.type("io.undertow.util.HttpString");
    response.getHeaders().add(HttpString.tryFromString("X-Facet-Plugin"), "request-logger");
    response.getHeaders().add(HttpString.tryFromString("X-Request-Path"), "" + request.getPath());
    response.getHeaders().add(HttpString.tryFromString("X-Request-Method"), "" + request.getMethod());
}
