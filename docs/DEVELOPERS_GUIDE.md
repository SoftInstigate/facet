# Facet - Developer's Guide

**Version:** 1.0.0-SNAPSHOT
**Last Updated:** 2026-01-15

This guide shows you how to build custom HTML interfaces that "decorate" your REST APIs using Facet — a lightweight, convention-based server-side rendering (SSR) framework. Facet integrates well with RESTHeart but the main theme is: add HTML views on top of your existing API.

**Framework Agnostic:** Facet works with any CSS framework (Bootstrap, Tailwind, Bulma, etc.) and any JavaScript framework (React, Vue, Alpine.js, vanilla JS, etc.). The examples in this guide use Bulma, Alpine.js, and HTMX for demonstration purposes, but these are not requirements.

---

## Table of Contents

1. [Understanding the SSR Framework](#understanding-the-ssr-framework)
2. [Template Structure & Conventions](#template-structure--conventions)
3. [Tutorial: Replacing the Global Browser](#tutorial-replacing-the-global-browser)
4. [Tutorial: Building Custom SSR Applications](#tutorial-building-custom-ssr-applications)
5. [Tutorial: Using Different Layouts](#tutorial-using-different-layouts)
6. [Tutorial: Selective SSR](#tutorial-selective-ssr)
7. [Template Context Variables](#template-context-variables)
8. [Working with HTMX](#working-with-htmx)
9. [Advanced Patterns](#advanced-patterns)

---

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

### Template Resolution Algorithm

**Full Page Requests** (no HTMX headers):
```
GET /mydb/products?page=1
Accept: text/html

Search order:
1. templates/mydb/products/index.html    ✓ (if exists)
2. templates/mydb/index.html              (parent fallback)
3. templates/index.html                    (global fallback)
4. No template → return JSON (the API remains unchanged)
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

### Current Structure

The following shows the structure of the **example browser application** included with Facet. Your application can use any structure and technologies you prefer.

```
templates/
├── layout.html              # Global layout (example: uses Bulma, Alpine.js, HTMX)
├── index.html               # Default MongoDB browser UI (full page)
├── error.html               # Global error page
├── _fragments/              # HTMX-routable fragments for partial updates
│   └── document-list-container.html
├── browser/
│   ├── _components/        # Reusable UI components (pure HTML)
│   │   ├── document-list-container.html
│   │   ├── breadcrumb.html
│   │   ├── pagination-controls.html
│   │   └── resource-list.html
│   └── login/, config/, status/
└── ping/
    └── index.html          # Ping service template
```

### Naming Conventions

- **`_fragments/`**: HTMX-routable fragments (underscore prefix for infrastructure directories)
- **`_components/`**: Reusable UI components (underscore prefix for infrastructure directories)

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
- **File names**: No underscore prefix (e.g., `document-list-container.html`, not `_document-list-container.html`)
- **`index.html`**: Full page resource template (maps to URL path)
- **`layout.html`**: Base layout template

### HTMX Fragment Routing

The browser supports parametric fragment routing for partial page updates:

- **HTMX requests** with `HX-Target` header are routed to fragment templates in `_fragments/`
- Fragment resolution: `HX-Target: #product-list` → `templates/_fragments/product-list.html`
- Fragments can be resource-specific: `templates/mydb/mycoll/_fragments/custom-view.html`
- Two-layer architecture:
  - **Fragment** (`_fragments/product-list.html`): HTMX entry point with re-initialization scripts
  - **Component** (`_components/product-list.html`): Pure HTML structure (reusable)

**Example**: Full page includes the fragment template to maintain DRY principle:
```html
{# index.html - Full page #}
{% extends "layout" %}
{% block main %}
    {% include "_fragments/document-list-container" %}
{% endblock %}

### HTMX Integration (2025)

This project added first-class HTMX support in 2025 to enable seamless partial updates and progressive enhancement. Below are the key implementation details and recommended template patterns.

- Server-side handling:
    - `HtmlResponseInterceptor` detects HTMX requests via the `HX-Request` and `HX-Target` headers and routes them to fragment rendering instead of full-page rendering.
    - `PathBasedTemplateResolver.resolveFragment()` maps `HX-Target` values to fragment templates under `_fragments` (e.g. `/_fragments/product-list` → `templates/.../_fragments/product-list.html`) with resource-specific and root fallbacks.
    - Fragment resolution is strict: missing fragment templates result in a 500 (strict mode) to surface template errors early; full-page requests fall back to JSON when no template exists.
    - Template existence is checked via the `TemplateProcessor.templateExists()` call before rendering (see `PathBasedTemplateResolver.tryTemplate()`).

- Template conventions:
    - Use `_fragments/` for HTMX entry-points (they may include re-initialization scripts).
    - Keep reusable markup in `_components/` (pure HTML) and include them from fragments or full pages.
    - Fragment example: `templates/mydb/products/_fragments/product-list.html` (HTMX entry point) and `templates/mydb/products/_components/product-card.html` (reusable component).

- Client-side behavior:
    - Fragments should re-run any required JS initialization after being swapped into the DOM (e.g., re-register framework-specific components or re-run event bindings, depending on your choice of JavaScript framework).
    - The example browser app's initialization logic lives in `src/main/resources/static/js/app.js` (registers components such as `documentListComponent`) — call the same init helpers from fragment templates or include a small inline script in the fragment that invokes the init function.

- Examples (server + client):
    - Request headers: `HX-Request: true` and `HX-Target: #product-list` (or use `hx-get`/`hx-target` on the client).
    - Template path mapping: `GET /mydb/products` with `HX-Target: #product-list` → `templates/mydb/products/_fragments/product-list.html` → falls back to `templates/_fragments/product-list.html`.

Add these notes when creating HTMX-enabled templates to ensure predictable server-side routing and consistent client-side re-initialization.
```

---

## Tutorial: Replacing the Global Browser

Want to replace the browser with your own UI framework? Here's how:

### Step 1: Create Your Layout

```html
<!-- templates/layout.html -->
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>{% block title %}My Custom Browser{% endblock %}</title>
    <!-- Your CSS framework (Bootstrap, Tailwind, etc.) -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
    {% block head %}{% endblock %}
</head>
<body>
    <nav class="navbar navbar-dark bg-dark">
        <div class="container-fluid">
            <a class="navbar-brand" href="/">My MongoDB Browser</a>
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

### Step 2: Create MongoDB Browser Template

```html
<!-- templates/index.html -->
{% extends "layout" %}

{% block title %}Database Browser{% endblock %}

{% block main %}
<div class="row">
    <div class="col-12">
        <h1>{{ database or 'Databases' }}</h1>

        {% if documents %}
        <table class="table table-striped">
            <thead>
                <tr>
                    <th>Document</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                {% for doc in documents %}
                <tr>
                    <td><pre>{{ doc | json_encode }}</pre></td>
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

**Done!** You've replaced the browser with your own UI framework.

---

## Tutorial: Building Custom SSR Applications

Let's build a custom admin UI for the `/admin/users` collection.

### Structure

```
templates/
├── layout.html              # Global layout (browser)
├── admin/
│   ├── _layouts/
│   │   └── layout.html      # Admin's own layout (React)
│   ├── _components/
│   │   └── user-table.html
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
            {% for user in documents %}
            <tr>
                <td>{{ user.username }}</td>
                <td>{{ user.email }}</td>
                <td>
                    {% for role in user.roles %}
                    <span class="badge">{{ role }}</span>
                    {% endfor %}
                </td>
                <td>
                    <span class="status-{{ user.status }}">{{ user.status }}</span>
                </td>
                <td>
                    <button onclick="editUser('{{ user._id }}')">Edit</button>
                    <button onclick="deleteUser('{{ user._id }}')">Delete</button>
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
- `GET /mydb/products` → Browser's global layout
- `GET /ping` → Ping's minimal layout

**Each application is independent!**

---

## Tutorial: Using Different Layouts

### Option 1: Service-Scoped Layout

Perfect for services with their own branding.

```
templates/
├── layout.html              # Browser's global layout
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
├── layout.html              # Full browser layout (example: Bulma, Alpine.js, HTMX)
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
curl -H "Accept: text/html" http://localhost:8080/mydb/products
→ HTML (if template exists)

# Request JSON explicitly
curl -H "Accept: application/json" http://localhost:8080/mydb/products
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

{% if sortBy %}
    <p>Sorted by: <code>{{ sortBy }}</code></p>
{% endif %}

{% if projection %}
    <p>Projection: <code>{{ projection }}</code></p>
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
| `projection` | String | Field projection |
| `sortBy` | String | Sort specification |
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

Facet provides custom Pebble filters for common path manipulation tasks (used by the example browser application):

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

Facet's example browser uses HTMX for partial page updates, providing a smooth single-page application experience without complex JavaScript frameworks.

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

### Best Practices

1. **Use declarative HTMX in templates** when possible (no Java needed)
2. **Use HtmxResponseHelper** only for server-controlled behaviors
3. **Choose event timing carefully**: `afterSwap` for most cases, `afterSettle` for animations
4. **Keep event names semantic**: `documentDeleted`, `exportComplete`, not `event1`
5. **Include relevant data** in event details for client handlers
6. **Document custom events** in template comments

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

## Facet Framework Event System

**For Facet Application Developers**: the example browser application implements a generic event system that allows you to extend functionality without modifying core code. The browser is just one application built on the Facet Framework - you can reuse these same patterns in your own Facet applications (including when deploying on RESTHeart).

### What is the Event System?

The event system allows you to listen for operations (document deletions, updates, etc.) and respond with custom logic. This enables:

- **Analytics tracking** - Send events to Google Analytics, Mixpanel, etc.
- **Audit logging** - Record all operations to a compliance database
- **Custom notifications** - Alert admins when critical data changes
- **Multi-part UI updates** - Update multiple components when data changes
- **Third-party integrations** - Sync changes to Elasticsearch, Redis, webhooks
- **Custom workflows** - Trigger business logic based on database operations

### Available Events

The browser dispatches standard events for MongoDB operations:

```javascript
import { BROWSER_EVENTS } from '/assets/js/utils/events.js';

// Document events
BROWSER_EVENTS.DOCUMENT_CREATED   // 'documentCreated'
BROWSER_EVENTS.DOCUMENT_UPDATED   // 'documentUpdated'
BROWSER_EVENTS.DOCUMENT_DELETED   // 'documentDeleted'

// Collection events
BROWSER_EVENTS.COLLECTION_CREATED  // 'collectionCreated'
BROWSER_EVENTS.COLLECTION_UPDATED  // 'collectionUpdated'
BROWSER_EVENTS.COLLECTION_DELETED  // 'collectionDeleted'

// Database events
BROWSER_EVENTS.DATABASE_CREATED    // 'databaseCreated'
BROWSER_EVENTS.DATABASE_DELETED    // 'databaseDeleted'
```

### Event Detail Structure

All events include structured detail information:

```javascript
{
    resource: 'document',      // Resource type
    action: 'deleted',         // Action performed
    database: 'mydb',          // Database name
    collection: 'users',       // Collection name
    docId: '507f1f77bcf...',  // Document ID (for document events)
    path: '/mydb/users',       // Request path
    user: 'admin',             // Current user (if available)
    timestamp: 1641234567890   // Unix timestamp
}
```

### Basic Usage

Listen to events using standard browser EventListener API:

```javascript
// Listen for document deletions
document.addEventListener('documentDeleted', (event) => {
    const { database, collection, docId, user, timestamp } = event.detail;

    console.log(`User ${user} deleted document ${docId} from ${database}/${collection}`);

    // Your custom logic here
    alert(`Document deleted: ${docId}`);
});
```

### Real-World Examples

#### Example 1: Analytics Tracking

Track all document deletions in Google Analytics:

```javascript
document.addEventListener('documentDeleted', (event) => {
    const { database, collection, docId } = event.detail;

    if (window.gtag) {
        gtag('event', 'document_deleted', {
            'event_category': 'database_operations',
            'event_label': `${database}/${collection}`,
            'value': 1
        });
    }
});
```

#### Example 2: Audit Logging

Send all database operations to an audit log:

```javascript
const AUDIT_EVENTS = [
    'documentCreated',
    'documentUpdated',
    'documentDeleted',
    'collectionCreated',
    'collectionDeleted'
];

AUDIT_EVENTS.forEach(eventName => {
    document.addEventListener(eventName, async (event) => {
        await fetch('/api/audit/log', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                action: event.detail.action,
                resource: event.detail.resource,
                user: event.detail.user,
                timestamp: event.detail.timestamp,
                details: event.detail
            })
        });
    });
});
```

#### Example 3: Custom Notifications

Alert admins when critical collections are modified:

```javascript
document.addEventListener('documentDeleted', (event) => {
    const { collection, docId } = event.detail;

    const criticalCollections = ['orders', 'payments', 'users'];

    if (criticalCollections.includes(collection)) {
        showNotification({
            type: 'warning',
            title: 'Important Document Deleted',
            message: `Document ${docId} was deleted from ${collection}`
        });

        // Notify admin via email/Slack
        fetch('/api/notify/admin', {
            method: 'POST',
            body: JSON.stringify({ collection, docId })
        });
    }
});
```

#### Example 4: Multi-Part UI Updates

Update multiple UI components when data changes:

```javascript
document.addEventListener('documentDeleted', (event) => {
    const { collection } = event.detail;

    // Update sidebar count
    const countElement = document.querySelector(`[data-collection="${collection}"] .count`);
    if (countElement) {
        const currentCount = parseInt(countElement.textContent);
        countElement.textContent = currentCount - 1;

        // Visual feedback
        countElement.classList.add('updated');
        setTimeout(() => countElement.classList.remove('updated'), 2000);
    }

    // Update dashboard statistics
    updateDashboardStats(collection, -1);

    // Update chart if present
    if (window.updateCollectionChart) {
        updateCollectionChart(collection);
    }
});
```

#### Example 5: Third-Party Integration

Sync deletions to external systems:

```javascript
document.addEventListener('documentDeleted', async (event) => {
    const { database, collection, docId } = event.detail;

    // Sync to Elasticsearch
    if (window.elasticsearch) {
        await elasticsearch.delete({
            index: `${database}_${collection}`,
            id: docId
        });
    }

    // Update Redis cache
    if (window.redis) {
        await redis.del(`doc:${database}:${collection}:${docId}`);
    }

    // Trigger webhook
    await fetch('/webhooks/trigger', {
        method: 'POST',
        body: JSON.stringify({
            event: 'document.deleted',
            payload: event.detail
        })
    });
});
```

#### Example 6: Custom Workflows

Trigger business logic based on collection-specific rules:

```javascript
document.addEventListener('documentDeleted', async (event) => {
    const { collection, docId } = event.detail;

    // When an order is deleted, notify customer service
    if (collection === 'orders') {
        await fetch('/api/customer-service/notify', {
            method: 'POST',
            body: JSON.stringify({
                type: 'order_deleted',
                orderId: docId,
                timestamp: event.detail.timestamp
            })
        });

        // Update inventory
        await updateInventory(docId);
    }

    // When a user is deleted, cleanup related data
    if (collection === 'users') {
        await cleanupUserData(docId);
    }
});
```

### Creating Custom Events in Your Facet App

Use the event utilities to dispatch your own events:

```javascript
import { dispatchResourceEvent, createEventDetail } from '/assets/js/utils/events.js';

// Dispatch a custom resource event
dispatchResourceEvent('product', 'purchased', {
    database: 'mydb',
    collection: 'products',
    docId: productId,
    quantity: 5
});

// Or dispatch any custom event
import { dispatchEvent } from '/assets/js/utils/events.js';

dispatchEvent('exportComplete', {
    filename: 'export.csv',
    rows: 1000
});
```

### Using Events in Templates

Include event listeners directly in your Pebble templates:

```html
<!-- templates/mydb/products/index.html -->
{% extends "layout" %}

{% block main %}
    <!-- Your main content -->
{% endblock %}

{% block script %}
<script type="module">
    // Listen for product-specific events
    document.addEventListener('documentDeleted', (event) => {
        if (event.detail.collection === 'products') {
            // Show confirmation message
            alert(`Product ${event.detail.docId} was deleted`);

            // Optionally reload product list
            location.reload();
        }
    });
</script>
{% endblock %}
```

### Complete Examples

For production-ready examples covering all common use cases, see:

**[docs/examples/event-listeners.html](../docs/examples/event-listeners.html)**

This file contains 9 complete examples:
1. Analytics tracking (Google Analytics)
2. Audit logging
3. Custom notifications
4. Multi-part UI updates
5. Third-party integrations
6. Rate limiting warnings
7. Backup deleted documents
8. Development debug mode
9. Custom workflow triggers

### Best Practices

1. **Namespace your event names** - Use prefixes for custom events: `myapp:actionComplete`
2. **Include relevant data** - Provide all context needed for handlers
3. **Handle errors gracefully** - Wrap async operations in try/catch
4. **Avoid blocking operations** - Events should not delay user actions
5. **Document custom events** - Comment what events your app dispatches and their data structure
6. **Use `cancelable: true`** - If you want handlers to be able to prevent default behavior

### Debugging Events

Enable debug logging in development:

```javascript
if (window.location.hostname === 'localhost') {
    const ALL_EVENTS = [
        'documentCreated', 'documentUpdated', 'documentDeleted',
        'collectionCreated', 'collectionDeleted', 'databaseDeleted'
    ];

    ALL_EVENTS.forEach(eventName => {
        document.addEventListener(eventName, (event) => {
            console.group(`[Event] ${eventName}`);
            console.log('Detail:', event.detail);
            console.log('Timestamp:', new Date(event.detail.timestamp).toISOString());
            console.groupEnd();
        });
    });
}
```

---

## Advanced Patterns

### Pattern 1: Template Composition

Break large templates into smaller components:

```
templates/
└── mydb/
    └── products/
        ├── index.html          # Main template
        └── _components/
            ├── product-card.html
            └── product-filters.html
```

```html
<!-- templates/mydb/products/index.html -->
{% extends "layout" %}

{% block main %}
    {% include "mydb/products/_components/product-filters" %}

    <div class="products">
        {% for product in documents %}
            {% include "mydb/products/_components/product-card" with {'product': product} %}
        {% endfor %}
    </div>
{% endblock %}
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
    <!-- Example: include your CSS framework and JavaScript libraries -->
    <link rel="stylesheet" href="/assets/css/vendor/bulma.min.css">
    <script src="/assets/js/vendor/alpinejs.min.js"></script>
{% endblock %}

{% block header %}
    <!-- Browser's navbar -->
{% endblock %}
```

### Pattern 3: Client-Side Enhancement

Example using Alpine.js (you can use any JavaScript framework):

```html
<!-- templates/mydb/products/index.html -->
{% extends "layout" %}

{% block main %}
<div x-data="{
    products: {{ documents | json_encode }},
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
{% if totalDocuments == 0 %}
    {% include "mydb/products/_components/empty-state" %}
{% elseif totalDocuments > 1000 %}
    {% include "mydb/products/_components/large-dataset-view" %}
{% else %}
    {% include "mydb/products/_components/standard-view" %}
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

<!-- Standard MongoDB browser UI -->
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
2. **Reuse Components**: Extract common UI elements into `_components/`
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

- Explore the browser's existing templates in `templates/browser/_components/`
- Check out `templates/index.html` for MongoDB browser implementation
- Read RESTHeart documentation on MongoDB mounts and authentication
- Experiment with different UI frameworks (React, Vue, Svelte)

---

## Resources

- **RESTHeart Documentation**: <https://restheart.org/docs/>
- **Pebble Template Engine**: <https://pebbletemplates.io/>
- **HTMX Documentation**: <https://htmx.org/>

### Technologies Used in the Example Browser

The included example browser application demonstrates Facet's capabilities using:
- **Alpine.js** for client-side interactivity: <https://alpinejs.dev/>
- **Bulma CSS** for styling: <https://bulma.io/>
- **HTMX** for partial page updates: <https://htmx.org/>

You can use any CSS framework (Bootstrap, Tailwind, Material UI, etc.) and any JavaScript framework (React, Vue, Svelte, vanilla JS, etc.) with Facet.

---

**Happy templating!** 🎨
