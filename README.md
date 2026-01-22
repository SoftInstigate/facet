# <img src="https://getfacet.org/assets/img/facet-logo.svg" alt="Facet logo" width="32px" height="auto" /> Facet

Server-side rendering for REST APIs using convention-based templates. Built on RESTHeart and MongoDB.

<img src="docs/decorate.png" alt="decorate API" width="98%" height="auto" />

## What is Facet?

Facet is a server-side rendering framework that maps REST API responses to HTML templates using path-based conventions. The same endpoint serves JSON or HTML based on the `Accept` header—no duplicate logic, no separate view layer.

**Core concept:** Request path = template path

```http
GET /shop/products
Accept: application/json  →  JSON response (REST API unchanged)
Accept: text/html         →  HTML rendered from templates/shop/products/index.html
```

Templates are opt-in. Add HTML rendering only where you need it; your REST API continues working unchanged.

## Quick Start

> ⚠️ We have not published a stable release yet, so the API is still subject to changes.

### Run the Example

```bash
# Clone and build
git clone https://github.com/SoftInstigate/facet.git
cd facet
mvn package -DskipTests

# Run the product catalog example
cd examples/product-catalog
docker-compose up
```

**Open:** http://localhost:8080/shop/products

**Login:** Admin (`admin`/`secret`) or Viewer (`viewer`/`viewer`)

**[→ Product Catalog Tutorial](docs/TUTORIAL_PRODUCT_CATALOG.md)** - Learn by exploring the working code

## Template Example

Here's the actual product list template from the example (simplified):

```html
{% extends "layout" %}

{% block main %}
<h1>Products</h1>

<form method="GET" hx-get="{{ path }}" hx-target="#product-list">
  <input type="text" name="search" placeholder="Search...">
  <button>Search</button>
</form>

<div id="product-list">
  {% for item in items %}
  <article>
    <h3>{{ item.data.name }}</h3>
    <p>{{ item.data.description }}</p>
    <span>${{ item.data.price }}</span>
  </article>
  {% endfor %}
</div>
{% endblock %}
```

**That's it.** No controllers, no services, no routing configuration. The template path matches the API path.

## Key Features

### Convention-Based Routing

Templates automatically resolve based on request path:

```http
GET /shop/products  →  templates/shop/products/index.html
GET /shop/products/123  →  templates/shop/products/view.html
```

Hierarchical fallback: if `shop/products/index.html` doesn't exist, tries `shop/index.html`, then `index.html`.

### HTMX Support Built In

Facet detects HTMX requests automatically and renders fragment templates:

```html
<!-- Full page template includes fragment -->
<div id="product-list">
  {% include "_fragments/product-list" %}
</div>

<!-- HTMX request with HX-Target: #product-list renders just the fragment -->
<a href="?sort=price" hx-get="?sort=price" hx-target="#product-list">
  Sort by Price
</a>
```

No backend code needed. Facet routes HTMX requests to fragment templates automatically.

### Hot Reload Templates

Edit templates, refresh browser, see changes. No restart required.

```yaml
/pebble-template-processor:
  use-file-loader: true    # Load from filesystem
  cache-active: false      # Disable cache in dev
```

### Rich Template Context

Templates have access to request data, MongoDB query parameters, pagination, and auth:

```html
<!-- Automatic pagination -->
<nav>
  Page {{ page }} of {{ totalPages }}
  {% if page < totalPages %}
    <a href="?page={{ page + 1 }}">Next</a>
  {% endif %}
</nav>

<!-- MongoDB query parameters available -->
{% if filter %}
  <p>Filtered by: {{ filter }}</p>
{% endif %}

<!-- Role-based rendering -->
{% if roles contains 'admin' %}
  <button>Delete</button>
{% endif %}
```

**[→ Template Context Reference](docs/TEMPLATE_CONTEXT_REFERENCE.md)** - Complete variable reference

## Developer Workflow

1. **MongoDB data** - Your collections are your data model
2. **REST API** - RESTHeart automatically exposes MongoDB as REST
3. **Templates** - Drop HTML templates where paths match resources
4. **Done** - Browsers get HTML, APIs get JSON

No routing files. No controller layer. No DTO mapping. Just templates and data.

## Technical Details

### Runtime Options

**GraalVM JDK** (default):
- ~1s startup time
- ~150MB memory footprint
- JavaScript plugin support via Polyglot API
- Full JVM capabilities

**Native Image** (optional):
- <100ms startup time
- ~50MB memory footprint
- No JavaScript plugins (GraalVM polyglot unavailable in native images)
- Optimal for cloud deployment

### Deployment

- **Format:** Single JAR or native binary
- **Packaging:** Docker, Kubernetes, bare metal
- **Architecture:** Stateless, horizontally scalable
- **Dependencies:** MongoDB only

### Performance

- Direct MongoDB streaming to templates (no ORM)
- ETag-based response caching
- Handles thousands of concurrent requests
- Production-tested on RESTHeart runtime

## Technology Stack

- **[RESTHeart](https://restheart.org)** - MongoDB REST API server (plugin architecture, auth, WebSocket)
- **[Pebble](https://pebbletemplates.io)** - Template engine (Twig/Jinja2-like syntax)
- **[GraalVM](https://www.graalvm.org)** - High-performance JDK with native compilation

## Use Cases

**Good fit:**
- Data-driven web applications (dashboards, admin panels, content sites)
- MongoDB-backed applications needing HTML views
- Projects wanting SSR without complex frameworks
- Teams preferring convention over configuration
- Applications requiring both API and web UI

**Not a fit:**
- Complex frontend state management (use React/Vue directly)
- Non-MongoDB databases (RESTHeart requires MongoDB)
- Applications with no REST API layer

## Documentation

- **[Product Catalog Tutorial](docs/TUTORIAL_PRODUCT_CATALOG.md)** - Guided walkthrough of complete example
- **[Developer's Guide](docs/DEVELOPERS_GUIDE.md)** - Architecture, patterns, advanced features
- **[Template Context Reference](docs/TEMPLATE_CONTEXT_REFERENCE.md)** - All available template variables
- **[Examples README](examples/README.md)** - How to run and create examples

## Examples

- **[Product Catalog](examples/product-catalog/)** - E-commerce with search, pagination, HTMX, auth

More examples coming soon.

## Comparison

### vs Spring Boot / Java MVC

- **Facet:** Convention-based templates, zero controllers, hot reload
- **Spring:** Explicit routing, controller layer, requires restart

### vs Laravel / Django / Rails

- **Facet:** JVM performance, stateless deployment, native image support
- **Script frameworks:** Rapid development but operational complexity at scale

### vs Next.js / Remix

- **Facet:** Server-side only, works with existing REST APIs, simpler mental model
- **Next.js:** Full-stack React, complex build process, frontend-focused

Facet occupies a unique position: JVM stability with scripting-language developer experience.

## Development Status

[![Java CI with Maven](https://github.com/SoftInstigate/facet/actions/workflows/build.yml/badge.svg)](https://github.com/SoftInstigate/facet/actions/workflows/build.yml)
[![GitHub releases](https://img.shields.io/github/v/release/SoftInstigate/facet?include_prereleases&label=latest%20release)](https://github.com/SoftInstigate/facet/releases)

Facet is in active development (pre-1.0). APIs may change.

**Track releases:** Click **Watch → Custom → Releases** to be notified of stable releases and breaking changes.

## Contributing

Contributions welcome! See open issues or propose new features via GitHub Discussions.

## License

Apache License 2.0

## Credits

Developed by [SoftInstigate](https://softinstigate.com) as a separate product from [RESTHeart](https://restheart.org).