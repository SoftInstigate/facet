# Facet - Developer's Guide

**Version:** RELEASE_VERSION
**Last Updated:** 2026-01-17

**Note:** `RELEASE_VERSION` is a textual placeholder. Replace it with the raw tag (for example, `0.1.0` or `1.0.0`) when cutting a release.

This guide provides a complete reference for Facet's features and capabilities. For a high-level overview, see the [README](../README.md).

## Overview

Facet is a data-driven web framework that transforms JSON documents into server-rendered HTML through path-based templates. It's a RESTHeart plugin that provides **hybrid API/UI from the same endpoint**—JSON for API clients, HTML for browsers.

**Core principle**: Convention over Configuration. Your template structure mirrors your API paths. A request to `/mydb/products` (collection) automatically uses `templates/mydb/products/list.html` or falls back to `index.html` when the browser requests HTML.

**Technology stack**:
- **[RESTHeart](https://restheart.org/)** - MongoDB REST API server (provides the plugin architecture, HTTP layer, auth, and more)
- **[Pebble](https://pebbletemplates.io/)** - Template engine with Twig/Jinja2-like syntax (if you know Jinja2, you know Pebble)
- **[GraalVM](https://www.graalvm.org/)** - High-performance JDK with optional native image compilation for fast startup

**Framework agnostic**: Use any CSS framework (Bootstrap, Tailwind, etc.) and any JavaScript framework (React, Vue, Alpine.js, vanilla JS) for client-side behavior. Facet only handles server-side rendering.

For advanced RESTHeart features (WebSocket, Change Streams, GridFS, custom auth, etc.), see the [RESTHeart documentation](https://restheart.org/docs/).

---

## Table of Contents

1. [Distribution & Releases](#distribution--releases)
2. [Understanding the SSR Framework](#understanding-the-ssr-framework)
3. [Template Structure & Conventions](#template-structure--conventions)
4. [Tutorial: Creating Your First Application](#tutorial-creating-your-first-application)
5. [Tutorial: Building Custom SSR Applications](#tutorial-building-custom-ssr-applications)
6. [Tutorial: Using Different Layouts](#tutorial-using-different-layouts)
7. [Tutorial: Selective SSR](#tutorial-selective-ssr)
8. [Template Context Variables](#template-context-variables)
9. [Working with HTMX](#working-with-htmx)
   - [HtmxResponseHelper (Server-Side Control)](#htmxresponsehelper-server-side-control)
   - [Handling Server-Triggered Events (Client-Side)](#handling-server-triggered-events-client-side)
   - [Best Practices](#best-practices)
10. [Advanced Patterns](#advanced-patterns)
11. [Configuration Reference](#configuration-reference)
12. [Troubleshooting](#troubleshooting)

---

## Distribution & Releases

Facet is distributed through **GitHub Releases** (binary artifacts) and **JitPack** (Maven coordinates). We publish **release tags only** to JitPack.

### JitPack (Maven)

- **Release tags only**: Tag releases with raw Maven versions (no `v` prefix). Example: `0.1.0` or `1.0.0`.
- **Coordinates**: `com.github.SoftInstigate:facet-core:RELEASE_VERSION`
- **Repository**: `https://jitpack.io`
- **JDK requirement**: JitPack builds use Java 25 (see [jitpack.yml](../jitpack.yml)).

### GitHub Releases (Binary Artifacts)

Each GitHub release should include:
- `facet-core.jar`
- `lib/` dependencies (the `core/target/lib` output)

These artifacts are used by the Docker build in [core/Dockerfile](../core/Dockerfile).

## Understanding the SSR Framework

### How It Works

The SSR framework automatically transforms REST APIs' JSON/BSON responses into HTML using **path-based template resolution** (commonly used with RESTHeart):

```
Request: GET /mydb/products
Accept: text/html

1. MongoService returns BSON data
2. HtmlResponseInterceptor intercepts
3. PathBasedTemplateResolver finds template
4. Pebble renders HTML
5. Browser receives HTML page
```

### Template Naming Convention

**Recommended Pattern (Explicit):**
- **`list.html`** - For collection views (recommended - clean, no conditional logic)
- **`view.html`** - For document views (recommended - clean, no conditional logic)
- **`index.html`** - Optional fallback (use when list/view can share template logic)

**Why explicit templates?** Templates are cleaner without conditional logic checking request type. File names clearly indicate purpose.

### Template Resolution Algorithm

**Collection Requests** (e.g., `/mydb/products`):
```
GET /mydb/products?page=1
Accept: text/html

Search order:
1. templates/mydb/products/list.html      (recommended - explicit collection view)
2. templates/mydb/products/index.html     (optional fallback)
3. templates/mydb/list.html               (parent fallback)
4. templates/mydb/index.html              (parent fallback)
5. templates/list.html                    (global collection template)
6. templates/index.html                   (global fallback)
7. No template → return JSON (the API remains unchanged)
```

**Document Requests** (e.g., `/mydb/products/123`):
```
GET /mydb/products/123
Accept: text/html

Search order:
1. templates/mydb/products/123/view.html  (document-specific override)
2. templates/mydb/products/view.html      (recommended - explicit document view)
3. templates/mydb/products/index.html     (optional fallback)
4. templates/mydb/view.html               (parent fallback)
5. templates/mydb/index.html              (parent fallback)
6. templates/view.html                    (global document template)
7. templates/index.html                   (global fallback)
8. No template → return JSON (the API remains unchanged)
```

**HTMX Fragment Requests** (with HX-Target header):
```
GET /mydb/products
HX-Request: true
HX-Target: #product-list

Search order:
1. templates/mydb/products/_fragments/product-list.html  (resource-specific)
2. templates/_fragments/product-list.html                 (root fallback)
3. No template → 500 error (strict mode)
```

**Key concepts**:
- Full page templates use hierarchical fallback (walks up parent directories)
- Fragment templates use 2-level fallback only (exact path or root)
 - If no full page template exists, the API returns JSON normally (no rendering change)
- If no fragment template exists, returns 500 error (strict mode)
- SSR is **opt-in per resource**

---

## Template Structure & Conventions

### Recommended Structure

Organize templates by resource path to match your API structure:

```
templates/
├── layout.html              # Your main layout
├── list.html                # Root resource template (e.g., databases list)
├── error.html               # Global error page
├── _fragments/              # HTMX-routable fragments (optional)
│   └── my-fragment.html
└── mydb/                    # Database-specific templates
    ├── list.html            # Collections list for 'mydb' (recommended)
    ├── view.html            # Database detail view (if needed)
    ├── index.html           # Optional fallback for both
    └── products/
        ├── list.html        # Product collection view (recommended)
        ├── view.html        # Single product detail view (recommended)
        └── index.html       # Optional fallback for both
```

**You can organize templates however you prefer** - this is just a recommended pattern that mirrors your API paths.

### Naming Conventions

- **`list.html`**: Collection view template (recommended - explicit, no conditional logic)
- **`view.html`**: Document view template (recommended - explicit, no conditional logic)
- **`index.html`**: Optional unified fallback (when list/view share logic, or for simple cases)
- **`_fragments/`**: HTMX-routable fragments (underscore prefix indicates not directly accessible via URL)
- **`layout.html`**: Base layout template using Pebble inheritance
- **Path-based**: Directory structure mirrors your API URLs

### Common Issues

#### Browser Authentication Popup on favicon.ico

**Problem:** Browsers automatically request `/favicon.ico` from the domain root, even when your HTML specifies `<link rel="icon" href="/assets/favicon.ico">`. If `/favicon.ico` requires authentication, this causes:
1. HTTP 401 response with `WWW-Authenticate` header
2. Browser shows authentication popup dialog
3. Annoying user experience

**Solution:** Always mount `favicon.ico` at the root path without authentication:

```yaml
/static-resources:
  - what: /static
    where: /assets
    embedded: true
  # Serve favicon at root to prevent browser auth popup
  - what: /static/favicon.ico
    where: /favicon.ico
    embedded: true
```

This serves the same favicon file at both `/assets/favicon.ico` (from your template) and `/favicon.ico` (browser automatic request), preventing authentication challenges.

### HTMX Fragment Routing

Facet supports HTMX fragment routing for partial page updates:

- **HTMX requests** with `HX-Target` header are routed to fragment templates in `_fragments/`
- Fragment resolution: `HX-Target: #product-list` → `templates/_fragments/product-list.html`
- Fragments can be resource-specific: `templates/mydb/products/_fragments/product-list.html`

**Example**: Full page can include fragment to maintain DRY principle:
```html
{# index.html - Full page #}
{% extends "layout" %}
{% block main %}
    {% include "_fragments/product-list" %}
{% endblock %}

### HTMX Integration

Facet added first-class HTMX support to enable seamless partial updates and progressive enhancement.

**Server-side handling:**
- `HtmlResponseInterceptor` detects HTMX requests via `HX-Request` and `HX-Target` headers
- Routes to fragment rendering instead of full-page rendering
- `PathBasedTemplateResolver.resolveFragment()` maps `HX-Target` values to fragment templates
- Fragment resolution is strict: missing fragments return 500 error to surface issues early
- Full-page requests without templates fall back to JSON

**Template conventions:**
- Use `_fragments/` for HTMX entry points
- Include JavaScript re-initialization if needed after swap
- Fragments can be resource-specific or use root fallback

**Example mapping:**
```
GET /mydb/products
HX-Request: true
HX-Target: #product-list

→ Tries: templates/mydb/products/_fragments/product-list.html
→ Falls back to: templates/_fragments/product-list.html
```

---

## Default Credentials & Security

RESTHeart includes a **default admin user** for development and testing:

- **Username:** `admin`
- **Password:** `secret`

**⚠️ IMPORTANT: This is a development credential only. Change this password before deploying to production!**

### Authentication and Authorization

RESTHeart separates **authentication** (who you are) from **authorization** (what you can do). Understanding this distinction is important for proper configuration.

#### File-Based Setup (Recommended for Examples)

The [product-catalog example](../examples/product-catalog/) demonstrates the recommended file-based approach:

**Authentication** - User credentials and role assignments in [users.yml](../examples/product-catalog/users.yml):
```yaml
users:
  - userid: admin
    password: secret
    roles:
      - admin
  - userid: viewer
    password: viewer
    roles:
      - viewer
```

**Authorization** - Role-based permissions (ACL) in [restheart.yml](../examples/product-catalog/restheart.yml):
```yaml
/fileAclAuthorizer:
  enabled: true
  permissions:
    - role: admin
      predicate: path-prefix[path=/]
      mongo:
        readFilter: null
        writeFilter: null  # Can read and write
    - role: viewer
      predicate: path-prefix[path=/]
      mongo:
        readFilter: null
        writeFilter: '{"_id": {"$exists": false}}'  # Read-only
```

**Configuration** - Enable file-based authentication:
```yaml
/fileRealmAuthenticator:
  enabled: true
  conf-file: /opt/restheart/etc/users.yml

/basicAuthMechanism:
  enabled: true
  authenticator: fileRealmAuthenticator
```

**Key Points:**
- `users.yml` contains ONLY credentials and role assignments
- `restheart.yml` contains permissions (what each role can do)
- Requires RESTHeart restart to apply changes
- Simple and secure for development and small deployments

#### MongoDB-Based Authentication (Alternative)

For dynamic user management:
- Users stored in MongoDB `restheart.users` collection
- Manage via REST API (no restart needed)
- To change password:
  ```bash
  curl -u admin:secret -X PATCH http://localhost:8080/restheart/users/admin \
    -H "Content-Type: application/json" \
    -d '{"password": "my-strong-password"}'
  ```

**Enable in restheart.yml:**
```yaml
/mongoRealmAuthenticator:
  enabled: true

/basicAuthMechanism:
  enabled: true
  authenticator: mongoRealmAuthenticator
```

#### Making Requests

All `curl` examples in this guide use `-u admin:secret` for HTTP Basic Authentication. This works with both authentication methods. In production, replace with your actual credentials.

#### Further Reading

For comprehensive security guidance including JWT authentication, LDAP/Active Directory, OAuth2/OIDC, and production best practices, see:
- [RESTHeart Security Fundamentals](https://restheart.org/docs/foundations/security-fundamentals)
- [RESTHeart Authentication](https://restheart.org/docs/security/authentication)
- [RESTHeart Authorization](https://restheart.org/docs/security/authorization)

---

## Tutorial: Creating Your First Application

Let's build a simple MongoDB data viewer application using Facet:

### Step 1: Create Your Layout

```html
<!-- templates/layout.html -->
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>{% block title %}My Application{% endblock %}</title>
    <!-- Your CSS framework (Bootstrap, Tailwind, etc.) -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
    {% block head %}{% endblock %}
</head>
<body>
    <nav class="navbar navbar-dark bg-dark">
        <div class="container-fluid">
            <a class="navbar-brand" href="/">My Data Viewer</a>
            {% if username %}
            <span class="text-light">{{ username }}</span>
            {% endif %}
        </div>
    </nav>

    <main class="container mt-4">
        {% block main %}{% endblock %}
    </main>

    {% block script %}{% endblock %}
</body>
</html>
```

### Step 2: Create Database List Template

```html
<!-- templates/index.html -->
{% extends "layout" %}

{% block title %}Databases{% endblock %}

{% block main %}
<div class="row">
    <div class="col-12">
        <h1>{{ db or 'Databases' }}</h1>

        {% if items %}
        <table class="table table-striped">
            <thead>
                <tr>
                    <th>Item</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                {% for item in items %}
                <tr>
                    <td><pre>{{ item | json_encode }}</pre></td>
                    <td>
                        <button class="btn btn-sm btn-primary">Edit</button>
                        <button class="btn btn-sm btn-danger">Delete</button>
                    </td>
                </tr>
                {% endfor %}
            </tbody>
        </table>
        {% endif %}

        <!-- Pagination -->
        {% if totalPages > 1 %}
        <nav>
            <ul class="pagination">
                {% for p in range(1, totalPages + 1) %}
                <li class="page-item {% if p == page %}active{% endif %}">
                    <a class="page-link" href="?page={{ p }}&pagesize={{ pagesize }}">{{ p }}</a>
                </li>
                {% endfor %}
            </ul>
        </nav>
        {% endif %}
    </div>
</div>
{% endblock %}
```

### Step 3: Create Error Page

```html
<!-- templates/error.html -->
{% extends "layout" %}

{% block title %}Error {{ statusCode }}{% endblock %}

{% block main %}
<div class="alert alert-danger">
    <h1>{{ statusCode }} - {{ statusMessage }}</h1>
    <p>Path: <code>{{ path }}</code></p>
    <a href="/" class="btn btn-primary">Go Home</a>
</div>
{% endblock %}
```

**Done!** You've created your first Facet application. Browse to any MongoDB resource and you'll see it rendered in HTML.

---

## Tutorial: Building Custom SSR Applications

Let's build a custom admin UI for the `/admin/users` collection.

### Structure

```
templates/
├── layout.html              # Global layout
├── admin/
│   ├── _layouts/
│   │   └── layout.html      # Admin's own layout
│   └── users/
│       └── index.html       # Custom UI for /admin/users
```

### Admin Layout (React-based)

```html
<!-- templates/admin/_layouts/layout.html -->
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>{% block title %}Admin Panel{% endblock %}</title>
    <script crossorigin src="https://unpkg.com/react@18/umd/react.production.min.js"></script>
    <script crossorigin src="https://unpkg.com/react-dom@18/umd/react-dom.production.min.js"></script>
    {% block head %}{% endblock %}
</head>
<body>
    <div id="admin-root">
        {% block main %}{% endblock %}
    </div>
    {% block script %}{% endblock %}
</body>
</html>
```

### Admin Users Template

```html
<!-- templates/admin/users/index.html -->
{% extends "admin/_layouts/layout" %}

{% block title %}User Management{% endblock %}

{% block main %}
<div class="admin-users">
    <h1>👥 User Management</h1>

    <table>
        <thead>
            <tr>
                <th>Username</th>
                <th>Email</th>
                <th>Roles</th>
                <th>Status</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody>
            {% for item in items %}
            <tr>
                <td>{{ item.data.username }}</td>
                <td>{{ item.data.email }}</td>
                <td>
                    {% for role in item.data.roles %}
                    <span class="badge">{{ role }}</span>
                    {% endfor %}
                </td>
                <td>
                    <span class="status-{{ item.data.status }}">{{ item.data.status }}</span>
                </td>
                <td>
                    <button onclick="editUser('{{ item._id.value }}')">Edit</button>
                    <button onclick="deleteUser('{{ item._id.value }}')">Delete</button>
                </td>
            </tr>
            {% endfor %}
        </tbody>
    </table>
</div>
{% endblock %}

{% block script %}
<script>
    function editUser(id) {
        // Your React component or vanilla JS
        alert('Edit user: ' + id);
    }

    function deleteUser(id) {
        if (confirm('Delete user?')) {
            fetch('/admin/users/' + id, { method: 'DELETE' })
                .then(() => location.reload());
        }
    }
</script>
{% endblock %}
```

### Result

- `GET /admin/users` → Admin's React-based layout
- `GET /mydb/products` → Application's global layout
- `GET /ping` → Ping's minimal layout

**Each area is independent!**

---

## Tutorial: Using Different Layouts

### Option 1: Service-Scoped Layout

Perfect for services with their own branding.

```
templates/
├── layout.html              # Application's global layout
└── ping/
    ├── _layouts/
    │   └── layout.html      # Ping's minimal layout
    └── index.html
```

```html
<!-- templates/ping/_layouts/layout.html -->
<!DOCTYPE html>
<html>
<head>
    <title>Ping Service</title>
    <style>
        body { font-family: monospace; padding: 20px; }
    </style>
</head>
<body>
    <h1>🏓 Ping Service</h1>
    {% block content %}{% endblock %}
</body>
</html>
```

```html
<!-- templates/ping/index.html -->
{% extends "ping/_layouts/layout" %}

{% block content %}
    <dl>
        <dt>Message:</dt><dd>{{ message }}</dd>
        <dt>Version:</dt><dd>{{ version }}</dd>
        <dt>Host:</dt><dd>{{ host }}</dd>
    </dl>
{% endblock %}
```

### Option 2: Alternative Global Layout

For lightweight services that share a simple style.

```
templates/
├── layout.html              # Main application layout
├── minimal-layout.html      # Simple layout (no frameworks)
└── ping/
    └── index.html          # Extends minimal-layout
```

```html
<!-- templates/minimal-layout.html -->
<!DOCTYPE html>
<html>
<head>
    <title>{% block title %}Facet{% endblock %}</title>
    <style>
        body { font-family: system-ui; max-width: 800px; margin: 2rem auto; }
    </style>
</head>
<body>
    {% block main %}{% endblock %}
</body>
</html>
```

```html
<!-- templates/ping/index.html -->
{% extends "minimal-layout" %}

{% block title %}Ping{% endblock %}

{% block main %}
    <h1>Ping Response</h1>
    <pre>{{ json }}</pre>
{% endblock %}
```

### Option 3: Standalone Template (No Layout)

Maximum control, no inheritance.

```html
<!-- templates/ping/index.html -->
<!DOCTYPE html>
<html>
<head>
    <title>Ping - {{ host }}</title>
</head>
<body>
    <h1>Ping</h1>
    <pre>{{ json }}</pre>
</body>
</html>
```

---

## Tutorial: Selective SSR

**Key insight**: SSR is **opt-in per resource**. If no template exists, the API returns JSON.

### Use Case: Mixed HTML/JSON API

```
templates/
├── layout.html
├── index.html              # HTML for MongoDB browsing
├── mydb/
│   └── products/
│       └── index.html     # Custom HTML for products
└── admin/
    └── users/
        └── index.html     # Custom HTML for user management

# Results:
GET /mydb/products       → HTML (template exists)
GET /mydb/orders         → JSON (no template)
GET /admin/users         → HTML (template exists)
GET /api/*               → JSON (no templates for /api)
```

### Configuration

No special configuration needed! Just create templates where you want HTML.

### Example: Progressive Enhancement

Start with JSON everywhere, add HTML gradually:

**Phase 1**: Everything returns JSON

```
# No templates
GET /mydb/products  → JSON
GET /mydb/users     → JSON
```

**Phase 2**: Add HTML for products

```
templates/mydb/products/index.html

GET /mydb/products  → HTML ✓
GET /mydb/users     → JSON (still)
```

**Phase 3**: Add HTML for users

```
templates/mydb/users/index.html

GET /mydb/products  → HTML ✓
GET /mydb/users     → HTML ✓
```

### Content Negotiation

Clients control the response format via `Accept` header:

```bash
# Request HTML (browser default)
curl -u admin:secret -H "Accept: text/html" http://localhost:8080/mydb/products
→ HTML (if template exists)

# Request JSON explicitly
curl -u admin:secret -H "Accept: application/json" http://localhost:8080/mydb/products
→ JSON (always available)
```

---

## Template Context Variables

Facet injects a rich set of variables into your templates, giving you access to request data, pagination info, permissions, and more.

**📖 For a complete reference of all available template variables, see:** [Template Context Reference](TEMPLATE_CONTEXT_REFERENCE.md)

### Quick Examples

#### Authentication

```html
{% if isAuthenticated %}
    <p>Logged in as: {{ username }}</p>
    <p>Roles: {{ roles | join(', ') }}</p>
    <a href="{{ loginUrl }}?logout">Logout</a>
{% else %}
    <a href="{{ loginUrl }}">Login</a>
{% endif %}
```

#### Resource Context

```html
<h1>
    {% if coll %}
        Collection: {{ db }}/{{ coll }}
    {% elseif db %}
        Database: {{ db }}
    {% else %}
        All Databases
    {% endif %}
</h1>

<p>Request path: {{ path }}</p>
<p>Resource type: {{ resourceType }}</p>
```

#### MongoDB Data

```html
{% if items %}
    <p>Showing {{ items.size }} items</p>

    {% for item in items %}
        {% if item.isString %}
            {# Database or collection name #}
            <div>{{ item.value }}</div>
        {% else %}
            {# Document #}
            <div class="document">
                <code>{{ item._id.value }}</code>
                <pre>{{ item.data | json_encode }}</pre>
            </div>
        {% endif %}
    {% endfor %}
{% else %}
    <p>No items found</p>
{% endif %}
```

#### Pagination

```html
<div class="pagination">
    <p>Page {{ page }} of {{ totalPages }}</p>
    <p>Total: {{ totalItems }} items</p>
    <p>Page size: {{ pagesize }}</p>

    {% if page > 1 %}
        <a href="?page={{ page - 1 }}&pagesize={{ pagesize }}">Previous</a>
    {% endif %}

    {% if page < totalPages %}
        <a href="?page={{ page + 1 }}&pagesize={{ pagesize }}">Next</a>
    {% endif %}
</div>
```

#### Query Parameters

```html
{% if filter %}
    <p>Filtered by: <code>{{ filter }}</code></p>
{% endif %}

{% if sort %}
    <p>Sorted by: <code>{{ sort }}</code></p>
{% endif %}

{% if keys %}
    <p>Field projection (keys): <code>{{ keys }}</code></p>
{% endif %}
```

#### Permission Checks

```html
{% if canCreateCollections %}
    <button>New Collection</button>
{% endif %}

{% if canDeleteDatabase %}
    <button class="danger">Delete Database</button>
{% endif %}
```

#### System Info

```html
<footer>
    <p>RESTHeart {{ version }} - Built {{ buildTime }}</p>
</footer>
```

### Key Variables Summary

| Variable | Type | Description |
|----------|------|-------------|
| `isAuthenticated` | Boolean | Whether user is authenticated |
| `username` | String | Authenticated user's name |
| `roles` | Set<String> | User's roles |
| `path` | String | Full request path with mount prefix |
| `mongoPath` | String | MongoDB resource path (prefix stripped) |
| `mongoPrefix` | String | MongoDB mount prefix (global) |
| `db` | String | Database name (resolved from mount or request) |
| `coll` | String | Collection name (resolved from mount or request) |
| `resourceType` | Enum | ROOT, DATABASE, COLLECTION, DOCUMENT |
| `items` | List | Enriched items (databases, collections, or documents) |
| `data` | String | JSON representation of response |
| `page` | Integer | Current page number |
| `pagesize` | Integer | Items per page |
| `totalPages` | Integer | Total number of pages |
| `totalItems` | Long | Total item count |
| `filter` | String | MongoDB filter query |
| `keys` | String | MongoDB field projection/keys |
| `sort` | String | MongoDB sort specification |
| `canCreate*` | Boolean | Permission flags for creating resources |
| `canDelete*` | Boolean | Permission flags for deleting resources |
| `version` | String | RESTHeart version number |
| `buildTime` | String | Build timestamp |

**📖 See [Template Context Reference](TEMPLATE_CONTEXT_REFERENCE.md) for:**
- Complete list of all variables
- Detailed descriptions and examples
- Enriched item structure details
- Common patterns and best practices
- Migration guide from older variable names

### Custom Pebble Filters

Facet provides custom Pebble filters for common path manipulation tasks:

#### `stripTrailingSlash`

Removes trailing slashes from paths (preserves root `/`):

```html
{{ "/api/testdb/" | stripTrailingSlash }}  {# → "/api/testdb" #}
{{ "/" | stripTrailingSlash }}              {# → "/" #}
{{ "/mydb" | stripTrailingSlash }}          {# → "/mydb" #}
```

**Use case**: Normalize paths before appending segments to avoid double slashes.

#### `buildPath(segment)`

Appends a segment to a base path with proper `/` handling:

```html
{{ "/api/testdb" | buildPath("mycoll") }}  {# → "/api/testdb/mycoll" #}
{{ "/" | buildPath("testdb") }}            {# → "/testdb" #}
{{ "" | buildPath("testdb") }}             {# → "/testdb" #}
```

**Use case**: Build navigation URLs that work with any MongoDB mount configuration.

### MongoDB Mount Prefix Support

Facet templates are mount-agnostic, meaning they work with MongoDB mounted at any path (`/`, `/api`, `/mongo`, etc.). When Facet runs on top of RESTHeart, this behavior matches RESTHeart's mount semantics.

#### Navigation Links Pattern

**Best practice** for building navigation links:

```html
{% set basePath = path | stripTrailingSlash %}
<a href="{{ basePath | buildPath(dbName) }}?page={{ page }}&pagesize={{ pagesize }}">
    {{ dbName }}
</a>
```

This works because `path` already contains the mount prefix.

#### Breadcrumb Navigation Pattern

**Best practice** for breadcrumb navigation:

```html
{% set prefix = mongoPrefix is defined ? mongoPrefix : '/' %}
{% set prefixPath = prefix == '/' ? '' : prefix %}
{% set segments = mongoPath | split('/') %}

<nav class="breadcrumb">
    <a href="{{ prefixPath }}/?page=1&pagesize={{ pagesize }}">Home</a>
    {% set currentPath = prefixPath %}
    {% for segment in segments %}
        {% if segment != '' %}
            {% set currentPath = currentPath | buildPath(segment) %}
            <a href="{{ currentPath }}">{{ segment }}</a>
        {% endif %}
    {% endfor %}
</nav>
```

#### ID Type Parameters

When building links to documents, only add `id_type` parameter for actual collection documents:

```html
{% if doc.string %}
    {# Database/Collection name (simple string) #}
    <a href="{{ basePath | buildPath(doc.value) }}?page={{ page }}&pagesize={{ pagesize }}">
        {{ doc.value }}
    </a>
{% else %}
    {# Document with _id field #}
    {% set needsIdType = doc._id.requiresParam and requestType == 'COLLECTION' %}
    <a href="{{ basePath | buildPath(doc._id.value) }}?page={{ page }}&pagesize={{ pagesize }}{% if needsIdType %}&id_type={{ doc._id.type }}{% endif %}">
        {{ doc._id.value }}
    </a>
{% endif %}
```

**Why?** Many REST APIs (RESTHeart included) return databases/collections as metadata documents with `_id` fields; these metadata documents are not actual collection documents and therefore should not include `id_type` parameters. Only real collection documents (when `requestType == 'COLLECTION'`) need `id_type` for non-ObjectId IDs.

---

## Working with HTMX

Facet has first-class HTMX support for partial page updates, providing smooth single-page application experience without complex JavaScript frameworks.

### HTMX in Templates (Declarative)

Templates control HTMX behavior using standard HTMX attributes. No Java code changes needed!

#### Basic Partial Update

```html
<!-- Load content into a container -->
<button hx-get="/api/data"
        hx-target="#result"
        hx-swap="innerHTML">
    Load Data
</button>

<div id="result">
    <!-- Content will be swapped here -->
</div>
```

#### Navigation with History

```html
<!-- Update content and URL -->
<a href="/mydb/mycoll"
   hx-get="/mydb/mycoll?page=1"
   hx-target="#main-content"
   hx-swap="outerHTML"
   hx-push-url="true">
    View Collection
</a>
```

#### Form Submission

```html
<!-- Submit form via HTMX -->
<form hx-post="/mydb/users"
      hx-target="#user-list"
      hx-swap="beforeend">
    <input name="name" required>
    <button type="submit">Add User</button>
</form>
```

#### Building Dynamic URLs in Templates

For page size changes or pagination:

```html
<!-- Pagination with all query params preserved -->
<select name="pagesize"
        hx-get="{{ path }}?page=1{% if filter %}&filter={{ filter | urlencode }}{% endif %}"
        hx-target="#document-list-container"
        hx-swap="outerHTML"
        hx-push-url="true"
        hx-vals='js:{"pagesize": event.target.value}'>
    <option value="10">10 per page</option>
    <option value="25">25 per page</option>
    <option value="50">50 per page</option>
</select>
```

### HtmxResponseHelper (Server-Side Control)

For **custom backend services** (for example, when integrating with RESTHeart), use `HtmxResponseHelper` to control client behavior from Java code.

> **Note**: This is for custom services you write, not for template-only customizations. If you're just creating templates, use declarative HTMX attributes (above) instead.

#### When to Use HtmxResponseHelper

Use server-side HTMX headers when your **Java service** needs to:

1. **Trigger client-side events** after operations
2. **Dynamically redirect** based on business logic
3. **Change swap behavior** conditionally
4. **Force page refresh** after critical changes

#### Tutorial: Custom Service with HTMX

Let's build a service that exports data and triggers a client notification:

```java
@RegisterPlugin(
    name = "export-service",
    description = "Exports collection data to CSV",
    enabledByDefault = true
)
public class ExportService implements JsonService {

    @Override
    public void handle(JsonRequest request, JsonResponse response) throws Exception {
        // Generate CSV export
        String csvData = generateCSV(request);

        // Return success message
        response.setContent(Json.object().put("message", "Export complete"));
        response.setStatusCode(200);

        // Trigger client-side event with export details
        JsonObject details = new JsonObject();
        details.addProperty("filename", "export.csv");
        details.addProperty("rows", csvData.lines().count());

        HtmxResponseHelper.triggerEventAfterSwap(
            response,
            "exportComplete",  // Event name
            details            // Event data
        );
    }
}
```

**Template to handle the event**:

```html
<!-- Listen for server-triggered event -->
<div hx-on:export-complete="handleExport(event.detail)">
    <button hx-post="/export"
            hx-target="#export-status"
            hx-swap="innerHTML">
        Export to CSV
    </button>

    <div id="export-status"></div>
</div>

<script>
function handleExport(details) {
    alert(`Exported ${details.rows} rows to ${details.filename}`);
}
</script>
```

#### Example: Dynamic Redirects

```java
@Override
public void handle(JsonRequest request, JsonResponse response) {
    String userId = createUser(request);

    // Redirect to the new user's profile
    String profileUrl = "/users/" + userId;
    HtmxResponseHelper.redirect(response, profileUrl);

    // Or replace URL without navigation
    // HtmxResponseHelper.replaceUrl(response, profileUrl);
}
```

#### Example: Conditional Retargeting

```java
@Override
public void handle(JsonRequest request, JsonResponse response) {
    boolean isError = performOperation(request);

    if (isError) {
        // Show error in notification area instead of main content
        HtmxResponseHelper.retarget(response, "#error-notifications");
        HtmxResponseHelper.reswap(response, "beforeend");
        response.setContent(errorMessage());
    } else {
        // Normal success response goes to original target
        response.setContent(successMessage());
    }
}
```

#### Example: Force Page Refresh

```java
@Override
public void handle(JsonRequest request, JsonResponse response) {
    // Update critical system configuration
    updateSystemConfig(request);

    // Force full page refresh to reload all data
    HtmxResponseHelper.refresh(response);
}
```

#### Example: Event After Database Operations

```java
@Override
public void handle(JsonRequest request, JsonResponse response) {
    // Delete multiple documents
    long deletedCount = deleteDocuments(request);

    response.setContent(Json.object().put("deleted", deletedCount));
    response.setStatusCode(200);

    // Trigger event to update document count in UI
    JsonObject details = new JsonObject();
    details.addProperty("deletedCount", deletedCount);
    details.addProperty("remainingCount", countRemaining());

    HtmxResponseHelper.triggerEventAfterSwap(
        response,
        "documentsDeleted",
        details
    );
}
```

**Template handler**:

```html
<body hx-on:documents-deleted="updateCounts(event.detail)">
    <script>
    function updateCounts(detail) {
        document.getElementById('doc-count').textContent = detail.remainingCount;
        if (detail.deletedCount > 0) {
            showToast(`${detail.deletedCount} documents deleted`);
        }
    }
    </script>
</body>
```

### Available HtmxResponseHelper Methods

```java
// Trigger client-side events
HtmxResponseHelper.triggerEvent(response, "eventName");
HtmxResponseHelper.triggerEvent(response, "eventName", jsonDetails);
HtmxResponseHelper.triggerEventAfterSwap(response, "eventName");
HtmxResponseHelper.triggerEventAfterSwap(response, "eventName", jsonDetails);
HtmxResponseHelper.triggerEventAfterSettle(response, "eventName");
HtmxResponseHelper.triggerEventAfterSettle(response, "eventName", jsonDetails);

// Control swap behavior
HtmxResponseHelper.retarget(response, "#different-target");
HtmxResponseHelper.reswap(response, "beforeend");  // innerHTML, outerHTML, beforeend, etc.

// Browser history
HtmxResponseHelper.pushUrl(response, "/new/path");      // Add to history
HtmxResponseHelper.replaceUrl(response, "/new/path");   // Replace current URL
HtmxResponseHelper.replaceUrl(response, "false");       // Prevent URL update

// Navigation
HtmxResponseHelper.redirect(response, "/login");  // Client-side redirect
HtmxResponseHelper.refresh(response);             // Force full page refresh
```

### HTMX Event Lifecycle

Understanding when to trigger events:

```java
// Immediately when response received (before DOM changes)
HtmxResponseHelper.triggerEvent(response, "beforeUpdate");

// After new content is swapped into DOM
HtmxResponseHelper.triggerEventAfterSwap(response, "contentUpdated");

// After DOM animations/transitions complete
HtmxResponseHelper.triggerEventAfterSettle(response, "animationDone");
```

### Handling Server-Triggered Events (Client-Side)

When you use `HtmxResponseHelper.triggerEvent()` from your Java service, HTMX dispatches these as standard DOM events that you can listen to in JavaScript.

#### Basic Event Handling

Listen to server-triggered events using the browser's `addEventListener` API:

```javascript
// Listen to event triggered by HtmxResponseHelper.triggerEvent()
document.addEventListener('exportComplete', (event) => {
    console.log('Export finished!');

    // event.detail contains the JSON details from server (if provided)
    if (event.detail) {
        console.log('Filename:', event.detail.filename);
        console.log('Rows:', event.detail.rows);
    }
});
```

#### Event Detail Structure

When you trigger an event with details from the server:

**Server (Java):**
```java
var details = new JsonObject();
details.addProperty("filename", "export.csv");
details.addProperty("rows", 1000);
HtmxResponseHelper.triggerEvent(response, "exportComplete", details);
```

**Client (JavaScript):**
```javascript
document.addEventListener('exportComplete', (event) => {
    // event.detail is the JsonObject you sent from server
    const { filename, rows } = event.detail;
    console.log(`Exported ${rows} rows to ${filename}`);
});
```

#### Complete Example: Document Deletion

**Server sends event after delete:**
```java
public void handle(ServiceRequest<?> request, ServiceResponse<?> response) {
    String docId = request.getPathParameter("docId");
    String collection = request.getPathParameter("collection");

    // Perform deletion
    deleteDocument(collection, docId);

    // Notify client with details
    var details = new JsonObject();
    details.addProperty("id", docId);
    details.addProperty("collection", collection);
    HtmxResponseHelper.triggerEventAfterSwap(response, "documentDeleted", details);

    response.setContent("<div class='alert'>Document deleted</div>");
}
```

**Client handles event:**
```javascript
document.addEventListener('documentDeleted', (event) => {
    const { id, collection } = event.detail;

    // Show notification
    showNotification(`Deleted document ${id} from ${collection}`);

    // Update UI counts
    const countEl = document.querySelector(`[data-collection="${collection}"] .count`);
    if (countEl) {
        countEl.textContent = parseInt(countEl.textContent) - 1;
    }

    // Analytics tracking (optional)
    if (window.gtag) {
        gtag('event', 'document_deleted', {
            collection: collection
        });
    }
});
```

#### Event Timing Control

Choose the right timing for your event based on when you need the client to act:

```javascript
// triggerEvent() - Fires IMMEDIATELY (before DOM swap)
// Use when: Need to prepare UI before content changes
document.addEventListener('beforeUpdate', (event) => {
    console.log('About to update DOM');
    // Disable buttons, show loading states, etc.
});

// triggerEventAfterSwap() - Fires AFTER DOM swap (most common)
// Use when: New content is in DOM, ready to interact with
document.addEventListener('contentUpdated', (event) => {
    console.log('DOM updated, new elements exist');
    // Update counters, init components, show notifications
});

// triggerEventAfterSettle() - Fires AFTER animations complete
// Use when: Need to wait for CSS transitions/animations
document.addEventListener('animationDone', (event) => {
    console.log('All transitions finished');
    // Scroll to element, focus input, etc.
});
```

#### Multiple Event Handlers

You can attach multiple handlers to the same event:

```javascript
// Handler 1: Update UI
document.addEventListener('documentDeleted', (event) => {
    updateDocumentList(event.detail.collection);
});

// Handler 2: Analytics
document.addEventListener('documentDeleted', (event) => {
    trackDeletion(event.detail.id);
});

// Handler 3: Show notification
document.addEventListener('documentDeleted', (event) => {
    showToast(`Deleted ${event.detail.id}`);
});
```

All three handlers will be called when the server triggers the event.

#### Organizing Event Handlers

For larger applications, organize event listeners in your JavaScript:

```javascript
// Put in your main app.js or similar
function initEventHandlers() {
    // Document operations
    document.addEventListener('documentCreated', handleDocumentCreated);
    document.addEventListener('documentUpdated', handleDocumentUpdated);
    document.addEventListener('documentDeleted', handleDocumentDeleted);

    // Export operations
    document.addEventListener('exportComplete', handleExportComplete);
    document.addEventListener('exportFailed', handleExportFailed);
}

function handleDocumentDeleted(event) {
    const { id, collection } = event.detail;
    showNotification(`Deleted ${id} from ${collection}`);
    refreshList(collection);
}

function handleExportComplete(event) {
    const { filename, rows } = event.detail;
    showNotification(`Exported ${rows} rows to ${filename}`);
    downloadFile(filename);
}

// Initialize when DOM ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initEventHandlers);
} else {
    initEventHandlers();
}
```

#### Optional: Reusable Event Utility

For cleaner code, you can create a small utility:

```javascript
// utils/events.js (optional)
export function onServerEvent(eventName, handler) {
    document.addEventListener(eventName, (event) => {
        handler(event.detail || {}, event);
    });
}

// Usage
import { onServerEvent } from './utils/events.js';

onServerEvent('documentDeleted', (detail) => {
    // detail is automatically extracted
    console.log('Deleted:', detail.id);
});

onServerEvent('exportComplete', ({ filename, rows }) => {
    console.log(`Exported ${rows} rows to ${filename}`);
});
```

This is completely optional - plain `addEventListener` works perfectly fine. Only add this if you find yourself writing many event handlers.

#### HTMX Built-in Events

In addition to your custom server-triggered events, HTMX provides built-in lifecycle events you can listen to:

```javascript
// Before HTMX makes the request
document.addEventListener('htmx:beforeRequest', (event) => {
    console.log('About to make request:', event.detail.xhr);
    // Show loading spinner
});

// After HTMX receives response
document.addEventListener('htmx:afterRequest', (event) => {
    console.log('Request complete');
    // Hide loading spinner
});

// After content is swapped into DOM
document.addEventListener('htmx:afterSwap', (event) => {
    console.log('Content swapped');
    // Reinitialize components if needed
});

// When HTMX encounters an error
document.addEventListener('htmx:responseError', (event) => {
    console.error('Request failed:', event.detail.xhr.status);
    // Show error notification
});
```

See [HTMX Events Reference](https://htmx.org/reference/#events) for complete list.

#### General Client-Side Events (Without Server)

You can also dispatch custom events entirely on the client side for UI coordination:

```javascript
// Dispatch custom event
function notifyDeletion(docId) {
    const event = new CustomEvent('documentDeleted', {
        detail: { id: docId },
        bubbles: true
    });
    document.dispatchEvent(event);
}

// Listen for it
document.addEventListener('documentDeleted', (event) => {
    console.log('Deleted:', event.detail.id);
});

// Trigger it
notifyDeletion('123');
```

This is useful for:
- Coordinating multiple UI components
- Decoupling modules
- Testing event handlers
- Progressive enhancement

### Best Practices

1. **Use declarative HTMX in templates** when possible (no Java needed)
2. **Use HtmxResponseHelper** only for server-controlled behaviors
3. **Choose event timing carefully**: `afterSwap` for most cases, `afterSettle` for animations
4. **Keep event names semantic**: `documentDeleted`, `exportComplete`, not `event1`
5. **Include relevant data** in event details for client handlers
6. **Document custom events** in template comments or README
7. **Test event handlers** by dispatching events manually in browser console

---

## CRUD Operations with HTMX Fragments

Facet uses path-based resolution that maps URLs to REST resources. For dynamic UI components like forms, use HTMX fragments to load them on demand while keeping URLs clean and REST-compliant.

### The Pattern

In Facet:
- **URLs represent resources**: `/products/{id}` maps to a MongoDB document
- **Templates match URLs**: [templates/products/view.html](../examples/product-catalog/templates/shop/products/view.html) renders the document
- **Forms are components**: [templates/_fragments/product-form.html](../examples/product-catalog/templates/_fragments/product-form.html) loaded via HTMX
- **HTTP methods are actions**: POST (create), GET (read), PATCH (update), DELETE (delete)

This keeps URLs REST-compliant and avoids routing conflicts with MongoDB document IDs.

### Complete Example: Product CRUD

The product catalog example demonstrates full CRUD functionality. Let's explore each operation.

#### View Product (GET)

**Template**: [templates/shop/products/view.html](../examples/product-catalog/templates/shop/products/view.html)

Displays product details at `/shop/products/{id}`:

```html
{% extends "layout" %}

{% block main %}
{% if documents is not empty %}
    {% set product = documents[0] %}

    <article>
        <header>
            <h1>{{ product.name }}</h1>
        </header>

        <h2 style="color: var(--pico-primary);">${{ product.price }}</h2>
        <p>{{ product.description }}</p>

        <footer>
            <button hx-get="{{ path }}"
                    hx-target="#product-form"
                    hx-swap="innerHTML">
                Edit Product
            </button>
        </footer>
    </article>

    <div id="product-form" style="display: none;"></div>
{% endif %}
{% endblock %}
```

**Key points**:
- URL stays clean: `/products/{id}`
- Form loads via HTMX into `#product-form` div
- Progressive enhancement: works with/without JavaScript

#### Edit Product (PATCH via HTMX Fragment)

**Fragment Template**: [templates/_fragments/product-form.html](../examples/product-catalog/templates/_fragments/product-form.html)

Loaded dynamically when user clicks "Edit Product":

```html
<article>
    <header>
        <h2>Edit Product</h2>
        <p><a href="{{ path | parentPath }}" class="secondary">← Back to Products</a></p>
    </header>

    <form id="productEditForm">
        <label>
            Name
            <input type="text" name="name" value="{{ product.data.name }}" required>
        </label>

        <label>
            Price ($)
            <input type="number" name="price" value="{{ product.data.price }}" step="0.01" required>
        </label>

        <div class="grid">
            <button type="submit">Save Changes</button>
            <button type="button" class="secondary" onclick="cancelEdit()">Cancel</button>
        </div>
    </form>
</article>

<script>
document.getElementById('productEditForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    const formData = new FormData(e.target);

    const data = {
        name: formData.get('name'),
        price: parseFloat(formData.get('price')),
        // ... other fields
    };

    const response = await fetch('{{ path }}', {
        method: 'PATCH',
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify(data)
    });

    if (response.ok) {
        // Remove query parameters and reload
        const url = new URL(globalThis.location.href);
        url.search = '';
        globalThis.location.href = url.toString();
    }
});

function cancelEdit() {
    document.getElementById('product-form').style.display = 'none';
    document.getElementById('product-details').style.display = 'block';
}
</script>
```

**HTMX Request Flow**:
1. User clicks "Edit Product" button with `hx-get="{{ path }}"`
2. HTMX sends request with `HX-Request: true` and `HX-Target: #product-form` headers
3. Facet resolves fragment: [templates/_fragments/product-form.html](../examples/product-catalog/templates/_fragments/product-form.html)
4. Form HTML loads into `#product-form` div
5. Form submits via `fetch()` with PATCH method to update document

**Fragment Resolution Order**:
1. `templates/shop/products/{documentId}/_fragments/product-form.html` (document-specific)
2. `templates/_fragments/product-form.html` ✅ (root fallback)

#### Create Product (POST via HTMX Fragment)

**Fragment Template**: [templates/_fragments/product-new.html](../examples/product-catalog/templates/_fragments/product-new.html)

Similar pattern for creating new products:

```html
<article>
    <header>
        <h2>Create New Product</h2>
        <p><a href="{{ path }}" class="secondary">← Back to Products</a></p>
    </header>

    <form id="productNewForm">
        <label>
            Name
            <input type="text" name="name" required>
        </label>

        <label>
            Price ($)
            <input type="number" name="price" step="0.01" required>
        </label>

        <div class="grid">
            <button type="submit">Create Product</button>
            <button type="button" class="secondary" onclick="cancelNew()">Cancel</button>
        </div>
    </form>
</article>

<script>
document.getElementById('productNewForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    const formData = new FormData(e.target);

    const data = {
        name: formData.get('name'),
        price: parseFloat(formData.get('price')),
        createdAt: new Date().toISOString()
    };

    const response = await fetch('{{ path }}', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify(data)
    });

    if (response.ok) {
        window.location.reload();
    }
});
</script>
```

**Triggered from list page** ([templates/shop/products/index.html](../examples/product-catalog/templates/shop/products/index.html)):

```html
<button hx-get="{{ path }}"
        hx-target="#product-new"
        hx-swap="innerHTML">
    + Add Product
</button>

<div id="product-new" style="display: none;"></div>
```

#### Delete Product (DELETE)

Delete operations use JavaScript `fetch()` with confirmation:

```html
<button class="secondary" onclick="deleteProduct()">Delete Product</button>

<script>
async function deleteProduct() {
    if (!confirm('Are you sure you want to delete this product?')) {
        return;
    }

    try {
        const response = await fetch('{{ path }}', {
            method: 'DELETE',
            credentials: 'include'
        });

        if (response.ok) {
            window.location.href = '{{ path | parentPath }}';
        } else {
            alert('Failed to delete product');
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}
</script>
```

### Query Parameters for Deep Linking

Use query parameters to create shareable links to specific UI states:

```html
<!-- In product list -->
<button onclick="window.location.href='{{ path }}/{{ doc._id }}?mode=edit'">
    Edit
</button>
```

**Auto-detect in view template**:

```javascript
globalThis.addEventListener('DOMContentLoaded', function() {
    const params = new URLSearchParams(globalThis.location.search);
    if (params.get('mode') === 'edit') {
        document.querySelector('button[hx-target="#product-form"]').click();
    }
});
```

This allows bookmarkable edit links like `/products/123?mode=edit` while keeping the resource URL clean.

### HTTP Methods Summary

RESTHeart's MongoDB API uses standard HTTP methods:

**Create (POST)**:
```javascript
const response = await fetch('/shop/products', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    },
    credentials: 'include',
    body: JSON.stringify(data)
});
```

**Read (GET)**:
```javascript
// Full page: browser navigation to /shop/products/{id}
// Fragment: HTMX loads template with HX-Request header
```

**Update (PATCH)**:
```javascript
const response = await fetch('/shop/products/{id}', {
    method: 'PATCH',
    headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    },
    credentials: 'include',
    body: JSON.stringify(data)
});
```

**Delete (DELETE)**:
```javascript
const response = await fetch('/shop/products/{id}', {
    method: 'DELETE',
    credentials: 'include'
});
```

### Benefits

This architectural pattern provides:

- **Clean URLs**: `/products/123` not `/products/123/edit`
- **No routing conflicts**: Document IDs never conflict with action names (e.g., a document with `_id: "edit"` works correctly)
- **Progressive enhancement**: Forms work with JavaScript, can fallback to traditional POST/redirect pattern
- **Separation of concerns**: Templates follow resource structure, forms are reusable components
- **REST compliance**: URLs represent resources, HTTP methods represent actions
- **Component reusability**: Same `product-form.html` fragment can be loaded from list page or detail page

### Architectural Benefits

**Clean REST URLs**:
```
✅ /products/{id}          - View product
✅ /products/{id}?mode=edit - View product with edit form open (shareable)
✅ /products               - Product list with add form
```

**No Routing Conflicts**:
With path-based resolution, there's never ambiguity:
- `/products/edit` is always document ID "edit" (a valid MongoDB document)
- Edit form loads via HTMX fragment, not URL routing
- `/products/new` is always document ID "new" (if it exists)
- Create form loads via HTMX fragment from `/products` list page

**Progressive Enhancement**:
- JavaScript enabled: Forms load inline via HTMX with smooth UX
- JavaScript disabled: Forms can still work with traditional POST/redirect pattern (with minor template changes)

**Component Reusability**:
- Fragment templates are reusable across different contexts
- Same `product-form.html` can be loaded from list page or detail page
- Consistent UI without code duplication

### Common Patterns

#### Pattern: Optimistic UI Updates

```html
<!-- Show loading state immediately, swap on response -->
<button hx-delete="/mydb/users/123"
        hx-target="closest .user-card"
        hx-swap="outerHTML swap:1s"
        hx-indicator="#loading">
    <span class="htmx-indicator" id="loading">Deleting...</span>
    <span>Delete User</span>
</button>
```

#### Pattern: Polling for Updates

```html
<!-- Poll every 5 seconds -->
<div hx-get="/api/status"
     hx-trigger="every 5s"
     hx-swap="innerHTML">
    Status: Loading...
</div>
```

#### Pattern: Dependent Requests

```html
<!-- Load details after selecting item -->
<select hx-get="/api/details"
        hx-target="#details"
        hx-trigger="change"
        hx-include="[name='userId']">
    <option value="1">User 1</option>
    <option value="2">User 2</option>
</select>

<div id="details">Select a user</div>
```

---

## Advanced Patterns

### Pattern 1: Template Composition

Break large templates into smaller reusable parts using Pebble's `{% include %}`:

```html
<!-- templates/mydb/products/index.html -->
{% extends "layout" %}

{% block main %}
    <h1>Products</h1>

    {% include "mydb/products/filters" %}

    <div class="products">
        {% for item in items %}
            {% include "mydb/products/product-card" with {'product': item.data} %}
        {% endfor %}
    </div>
{% endblock %}
```

```html
<!-- templates/mydb/products/product-card.html -->
<div class="product-card">
    <h3>{{ product.name }}</h3>
    <p>{{ product.description }}</p>
    <span class="price">${{ product.price }}</span>
</div>
```

### Pattern 2: Layout Inheritance Hierarchy

```html
<!-- templates/minimal-layout.html (base) -->
<!DOCTYPE html>
<html>
<head>
    {% block head %}
    <title>{% block title %}Facet{% endblock %}</title>
    {% endblock %}
</head>
<body>
    {% block body %}
        <header>{% block header %}{% endblock %}</header>
        <main>{% block main %}{% endblock %}</main>
        <footer>{% block footer %}{% endblock %}</footer>
    {% endblock %}
</body>
</html>
```

```html
<!-- templates/layout.html (extends minimal) -->
{% extends "minimal-layout" %}

{% block head %}
    {{ parent() }}  <!-- Include parent's head -->
    <!-- Add your CSS framework and JavaScript libraries -->
    <link rel="stylesheet" href="/assets/css/styles.css">
    <script src="/assets/js/app.js"></script>
{% endblock %}

{% block header %}
    <!-- Your navbar here -->
{% endblock %}
```

### Pattern 3: Client-Side Enhancement

Example using Alpine.js (you can use any JavaScript framework):

```html
<!-- templates/mydb/products/index.html -->
{% extends "layout" %}

{% block main %}
<div x-data="{
    products: {{ items | map(i => i.data) | json_encode }},
    filter: '',
    get filteredProducts() {
        return this.products.filter(p =>
            p.name.toLowerCase().includes(this.filter.toLowerCase())
        );
    }
}">
    <input type="text"
           x-model="filter"
           placeholder="Search products...">

    <template x-for="product in filteredProducts" :key="product._id">
        <div class="product-card">
            <h3 x-text="product.name"></h3>
            <p x-text="product.description"></p>
            <button @click="addToCart(product)">Add to Cart</button>
        </div>
    </template>
</div>
{% endblock %}
```

### Pattern 4: Conditional Template Loading

Different templates based on conditions:

```html
<!-- templates/mydb/products/index.html -->
{% extends "layout" %}

{% block main %}
{% if totalItems == 0 %}
    <div class="empty-state">
        <p>No products found</p>
    </div>
{% elseif totalItems > 1000 %}
    <p>Large dataset: {{ totalItems }} products. Use filters to narrow results.</p>
    {% include "mydb/products/list-view" %}
{% else %}
    {% include "mydb/products/list-view" %}
{% endif %}
{% endblock %}
```

### Pattern 5: Multi-Tenant Templates

Using parametric mounts:

```yaml
# Configuration
mongo-mounts:
  - what: "/{host[0]}/{*}"  # tenant-based routing
    where: "/{*}"
```

```html
<!-- templates/index.html -->
{% extends "layout" %}

{% block main %}
{% if tenantId %}
    <h1>{{ tenantId | capitalize }} Database</h1>
    <p class="tenant-notice">
        You're viewing data for tenant: <strong>{{ tenantId }}</strong>
    </p>
{% endif %}

<!-- Standard data view -->
{% endblock %}
```

---

## Configuration Reference

### Pebble Template Processor

```yaml
/pebble-template-processor:
  enabled: true

  # Load from filesystem (for hot-reload)
  use-file-loader: false           # Default: false (classpath)

  # Custom templates location
  templates-path: /opt/templates/  # Default: src/main/resources/templates/

  # Template caching
  cache-active: true               # Default: true

  # Locale
  locale: en-US                    # Default: server locale
```

### HTML Response Interceptor

```yaml
/html-response-interceptor:
  enabled: true

  # Response caching
  response-caching: true           # Default: true
  max-age: 5                       # Cache-Control max-age (seconds)
```

---

## Best Practices

1. **Start Simple**: Begin with minimal templates, add complexity as needed
2. **Reuse Templates**: Use Pebble's `{% include %}` to extract common UI elements
3. **Scope Layouts**: Each major application should have its own layout
4. **Test Both Formats**: Ensure JSON API still works (use `Accept: application/json`)
5. **Progressive Enhancement**: Add HTML gradually, keep JSON as fallback
6. **Use Content Negotiation**: Let clients choose HTML or JSON
7. **Keep Templates Maintainable**: Break large templates into smaller includes
8. **Document Context Variables**: Comment what variables your templates expect

---

## Troubleshooting

### Template Not Found

**Problem**: Request returns JSON instead of HTML

**Solution**: Check template exists at correct path

```bash
GET /mydb/products
# Should have template at: templates/mydb/products/index.html
# Or fallback: templates/mydb/index.html
# Or global: templates/index.html
```

### Wrong Layout Applied

**Problem**: Template uses wrong layout

**Solution**: Check `{% extends %}` directive

```html
<!-- templates/ping/index.html -->
{% extends "layout" %}              <!-- Global layout -->
{% extends "ping/_layouts/layout" %} <!-- Ping's layout -->
{% extends "minimal-layout" %}       <!-- Alternative global -->
```

### Template Changes Not Appearing

**Problem**: Updates not reflected

**Solution**: Check caching configuration

```yaml
/pebble-template-processor:
  cache-active: false      # Disable for development
  use-file-loader: true    # Load from filesystem
```

### Missing Context Variables

**Problem**: Variables undefined in template

**Solution**: Check spelling and availability

```html
<!-- Debug: Show all available variables -->
{% for key, value in _context %}
    <p>{{ key }}: {{ value }}</p>
{% endfor %}
```

---

## Next Steps

- Check [Template Context Reference](TEMPLATE_CONTEXT_REFERENCE.md) for all available variables
- Read [RESTHeart documentation](https://restheart.org/docs/) for advanced features (auth, WebSocket, Change Streams, GridFS)
- Experiment with different UI frameworks (React, Vue, Alpine.js, vanilla JS)
- Build your first application following the tutorials in this guide

---

## Resources

- **RESTHeart Documentation**: <https://restheart.org/docs/>
- **Pebble Template Engine**: <https://pebbletemplates.io/>
- **HTMX Documentation**: <https://htmx.org/>

### Framework Agnostic

Facet works with any CSS framework (Bootstrap, Tailwind, Bulma, Pico CSS, Material UI, etc.) and any JavaScript framework (React, Vue, Alpine.js, Svelte, vanilla JS, etc.). The product catalog example (`examples/product-catalog`) uses Pico CSS v2, a semantic classless framework. Choose the tools that fit your project.

---

**Happy templating!** 🎨
