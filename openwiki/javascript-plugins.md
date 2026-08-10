---
type: Guide
title: JavaScript Server-Side Plugins
description: How to extend Facet with JavaScript and TypeScript plugins — writing RESTHeart services and interceptors in JavaScript, hot-reload workflow, GraalVM interop with MongoDB, and pairing JS services with Facet templates.
tags: [javascript, plugins, graalvm, restheart, service, interceptor, typescript, hot-reload]
---

# JavaScript Server-Side Plugins

Facet runs on top of RESTHeart, which supports writing plugins in JavaScript (and TypeScript) using GraalVM or RESTHeart native. This means you can add custom backend logic — computed endpoints, data transformations, access-control hooks — **without writing any Java** and **without a compilation step**.

See the [RESTHeart JavaScript plugins documentation](https://restheart.org/docs/framework/javascript-plugins) for the full runtime API reference.

## Why JavaScript Plugins?

| Aspect | JavaScript plugin | Java plugin |
|---|---|---|
| Language | JavaScript / TypeScript (ESM) | Java |
| Compilation | None — edit and reload | `mvn package` + container restart |
| Hot-reload | ✅ Yes (next request picks up edits) | ❌ No |
| MongoDB access | Via `mclient` global (GraalVM interop) | Via `MongoClient` injection |
| RESTHeart API | Same `request` / `response` objects | Same `ServiceRequest` / `ServiceResponse` |
| Deployment | Mount a directory; RESTHeart scans it | Copy JAR to plugins directory |
| Best for | Computed endpoints, data aggregation, rapid iteration | Complex plugins, shared libraries, performance-critical code |

## Plugin Types

RESTHeart exposes two kinds of extension points for JavaScript:

- **Service** — adds a new HTTP endpoint (maps a URI to a handler function).
- **Interceptor** — hooks into the request/response lifecycle for existing endpoints (validate input, enrich responses, log, etc.).

## Writing a Service

A service is an ES module (`.mjs`) that exports an `options` object and a `handle` function.

```javascript
// my-service.mjs

export const options = {
    name: "myService",          // internal plugin name
    description: "My service",
    uri: "/api/my-endpoint",    // HTTP path
    secured: true,              // require authentication
    matchPolicy: "EXACT"        // EXACT | PREFIX | REGEX
};

export function handle(request, response) {
    const result = { message: "Hello from JavaScript!" };
    response.setContent(JSON.stringify(result));
    response.setContentTypeAsJson();
}
```

### Accessing MongoDB

RESTHeart injects a `mclient` global (a `com.mongodb.client.MongoClient` instance). Use GraalVM's `Java.type()` to import Java classes.

```javascript
const BsonDocument = Java.type("org.bson.BsonDocument");

export function handle(request, response) {
    const db   = mclient.getDatabase("mydb");
    const coll = db.getCollection("mycollection", BsonDocument.class);

    let results = [];
    const it = coll.find().limit(10).iterator();
    while (it.hasNext()) {
        results.push(JSON.parse(it.next().toJson()));
    }

    response.setContent(JSON.stringify(results));
    response.setContentTypeAsJson();
}
```

### Declaring the Service in `package.json`

Each plugin directory must contain a `package.json` that lists which `.mjs` files are services and which are interceptors:

```json
{
  "name": "my-plugin",
  "version": "1.0.0",
  "description": "My JavaScript plugin",
  "rh:services": ["my-service.mjs"]
}
```

Use `rh:interceptors` for interceptor files.

## Writing an Interceptor

An interceptor exports `options` (with `interceptPoint`) and a `handle` function. It also exports a `resolve` predicate that determines which requests the interceptor applies to.

```javascript
// my-interceptor.mjs

export const options = {
    name: "myInterceptor",
    description: "Adds a custom response header",
    interceptPoint: "RESPONSE"   // REQUEST_BEFORE_AUTH | REQUEST_AFTER_AUTH | RESPONSE | RESPONSE_ASYNC
};

export function resolve(request) {
    return request.getPath().startsWith("/api/");
}

export function handle(request, response) {
    response.getHeaders().add("X-Powered-By", "Facet");
}
```

Declare it in `package.json`:

```json
{
  "name": "my-interceptor-plugin",
  "version": "1.0.0",
  "rh:interceptors": ["my-interceptor.mjs"]
}
```

## Folder Layout and Deployment

Place each plugin in its own directory under `plugins/`. RESTHeart scans this directory at startup.

```
plugins/
└── my-plugin/
    ├── package.json       ← declares services/interceptors
    └── my-service.mjs     ← the implementation
```

### Docker Compose

Mount the plugins directory as a volume so RESTHeart can load the plugins and hot-reload edits:

```yaml
services:
  facet:
    image: softinstigate/facet:latest
    volumes:
      - ./plugins/my-plugin:/opt/restheart/plugins/my-plugin:ro
      # ... other mounts
```

You can mount multiple plugin directories independently:

```yaml
volumes:
  - ./plugins/stats:/opt/restheart/plugins/stats:ro
  - ./plugins/hooks:/opt/restheart/plugins/hooks:ro
```

### Hot Reload

Edit any `.mjs` file while the stack is running. The next HTTP request to that endpoint automatically uses the updated code — no container restart, no Maven build.

```bash
# Make an edit
vim plugins/my-plugin/my-service.mjs

# The next request picks it up automatically
curl http://localhost:8080/api/my-endpoint
```

## Pairing a JavaScript Service with a Facet Template

A JavaScript service returns JSON. Facet intercepts the response and renders an HTML template when the request has `Accept: text/html` (browsers) or HTMX headers. This means you get both a JSON API and an HTML page from the same endpoint with no extra work.

1. **Write the service** returning JSON at `/shop/stats`.
2. **Create the template** at `templates/shop/stats/index.html`.

Facet resolves the template by path: the service URI `/shop/stats` maps to `templates/shop/stats/index.html` (or `list.html` / `view.html` for collection/document endpoints). See [Template System](template-system.md) for the full resolution algorithm.

Example template:

```html
{% extends "layout" %}
{% block main %}
<h1>Statistics</h1>
<p>Total products: {{ total }}</p>
<p>Average price: ${{ avgPrice }}</p>
{% endblock %}
```

Each top-level JSON key returned by the service becomes a direct template variable — no `document.` prefix needed. Facet's `JsonHtmlResponseHandler` merges all service data keys into the template context via `withServiceData()`.

```bash
# JSON response (API clients)
curl -u admin:secret http://localhost:8080/shop/stats

# HTML response (browsers / Facet)
curl -u admin:secret -H "Accept: text/html" http://localhost:8080/shop/stats
```

## Working Examples

The [product-catalog example](../examples/product-catalog/) includes both a JavaScript service and an interceptor:

### Service: Product Statistics

| File | Description |
|---|---|
| [`plugins/product-stats/product-stats.mjs`](../examples/product-catalog/plugins/product-stats/product-stats.mjs) | Service that aggregates product stats from MongoDB |
| [`plugins/product-stats/package.json`](../examples/product-catalog/plugins/product-stats/package.json) | Plugin declaration (`rh:services`) |
| [`templates/shop/stats/index.html`](../examples/product-catalog/templates/shop/stats/index.html) | Facet template for the HTML dashboard |

### Interceptor: Request Logger

| File | Description |
|---|---|
| [`plugins/request-logger/request-logger.mjs`](../examples/product-catalog/plugins/request-logger/request-logger.mjs) | Interceptor that adds diagnostic response headers to `/shop/` endpoints |
| [`plugins/request-logger/package.json`](../examples/product-catalog/plugins/request-logger/package.json) | Plugin declaration (`rh:interceptors`) |

To run both:

```bash
cd examples/product-catalog
docker compose up
```

Then open http://localhost:8080/shop/stats in a browser (login: `admin` / `secret`). Inspect the response headers to see the interceptor in action (`X-Facet-Plugin`, `X-Request-Path`, `X-Request-Method`).

## TypeScript

RESTHeart supports TypeScript with transpilation at load time when using GraalVM. Use `.ts` files and declare them in `package.json` just like `.mjs` files. Refer to the [RESTHeart JavaScript plugins documentation](https://restheart.org/docs/framework/javascript-plugins) for TypeScript-specific setup.

## Security Considerations

- Set `secured: true` in the `options` object to require authentication. Unauthenticated requests receive `401 Unauthorized`.
- Use RESTHeart's ACL to restrict access by role. See the [Operations & Deployment](operations.md) page for ACL configuration.
- Validate all request parameters inside `handle` before passing them to MongoDB queries.

## Limitations and Caveats

- **Docker image requirement**: RESTHeart 9.7.x had multiple bugs preventing JS plugins from loading. Use `softinstigate/restheart-snapshot:diagnostic` or later until a stable release includes the fixes. See [restheart#663](https://github.com/SoftInstigate/restheart/issues/663) for details.
- Hot-reload requires the plugins directory to be mounted as a writable volume and RESTHeart to be running on GraalVM. Static native builds do not support hot-reload.
- The `mclient` global is available only in services and interceptors running inside RESTHeart; it is not available during build or test time.
- Large or CPU-intensive computations are better suited to Java plugins, which benefit from JIT compilation.

## Further Reading

- [RESTHeart JavaScript plugins](https://restheart.org/docs/framework/javascript-plugins) — full runtime API, TypeScript support, and advanced examples
- [Template System](template-system.md) — how Facet resolves templates for service responses
- [HTMX Integration](htmx.md) — combine JavaScript services with HTMX fragments
- [Operations & Deployment](operations.md) — Docker setup, volumes, and RESTHeart configuration
