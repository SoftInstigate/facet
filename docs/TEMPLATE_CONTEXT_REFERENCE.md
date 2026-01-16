# Template Context Reference

This document provides a complete reference for all variables available in Facet templates.

## Table of Contents

- [Global Context Variables](#global-context-variables)
- [Authentication Context](#authentication-context)
- [MongoDB Request Context](#mongodb-request-context)
- [Pagination Context](#pagination-context)
- [Permission Context](#permission-context)
- [Multi-Tenant Context](#multi-tenant-context)
- [Generic JSON Service Context](#generic-json-service-context)
- [Enriched Items Structure](#enriched-items-structure)
- [Common Patterns](#common-patterns)

---

## Global Context Variables

These variables are available in **all templates** throughout the application. They are set during plugin initialization and remain constant during the application lifecycle.

| Variable | Type | Description | Example |
|----------|------|-------------|---------|
| `version` | String | RESTHeart version number | `"8.1.0"` |
| `buildTime` | String | Build timestamp | `"2024-01-15T10:30:00Z"` |
| `mongoPrefix` | String | MongoDB mount point from configuration | `"/"`, `"/api"`, `"/data"` |
| `loginUrl` | String | Login page URL for authentication | `"/login"` |
| `rolesUrl` | String | Roles endpoint URL for authorization | `"/roles"` |
| `baseUrl` | String | Base URL for the application | `"/browser"` |

### Set By
- `version`, `buildTime`: [PebbleTemplateProcessorProvider](../core/src/main/java/org/facet/templates/pebble/PebbleTemplateProcessorProvider.java)
- `loginUrl`, `rolesUrl`: [LoginService](../core/src/main/java/org/facet/html/LoginService.java)
- `mongoPrefix`, `baseUrl`: Application services (e.g., BrowserService)

### Usage Example
```html
<footer>
  <p>RESTHeart v{{ version }} - Built {{ buildTime }}</p>
  <a href="{{ loginUrl }}">Login</a>
</footer>
```

---

## Authentication Context

These variables are **conditionally set** based on whether a user is authenticated.

| Variable | Type | Description | When Set |
|----------|------|-------------|----------|
| `isAuthenticated` | Boolean | Whether user is authenticated | Always |
| `username` | String | Authenticated user's principal name | If authenticated |
| `roles` | Set&lt;String&gt; | User's role names | If authenticated |

### Set By
[TemplateContextBuilder.withAuthenticatedUser()](../core/src/main/java/org/facet/templates/TemplateContextBuilder.java)

### Usage Example
```html
{% if isAuthenticated %}
  <p>Welcome, {{ username }}!</p>
  {% if "admin" in roles %}
    <a href="/admin">Admin Panel</a>
  {% endif %}
  <a href="{{ loginUrl }}?logout">Logout</a>
{% else %}
  <a href="{{ loginUrl }}">Login</a>
{% endif %}
```

---

## MongoDB Request Context

These variables provide information about the current MongoDB request and resource.

### Path Variables

| Variable | Type | Description | Example |
|----------|------|-------------|---------|
| `path` | String | Full request path including prefix | `"/api/mydb/products"` |
| `mongoPath` | String | MongoDB resource path (prefix stripped) | `"/mydb/products"` |
| `resourceUrl` | String | Resolved MongoDB resource URL | `"/mydb/products"` |
| `collectionUrl` | String | Collection-level URL for navigation | `"/api/mydb/products"` |

### Resource Identification

| Variable | Type | Description | Example |
|----------|------|-------------|---------|
| `resourceType` | String | Type of MongoDB resource | `"ROOT"`, `"DATABASE"`, `"COLLECTION"`, `"DOCUMENT"` |
| `db` | String | Current database name (resolved from mount or request) | `"mydb"` |
| `coll` | String | Current collection name (resolved from mount or request) | `"products"` |

**Note**: `db` and `coll` are the **resolved** values, accounting for parametric mounts. Templates no longer need to check both mounted and non-mounted versions.

### Data Variables

| Variable | Type | Description |
|----------|------|-------------|
| `items` | List&lt;Map&gt; | List of enriched items (databases, collections, or documents) |
| `data` | String | Raw JSON response as a string |

**Note**: The `items` list contains different types depending on `resourceType`:
- `ROOT`: Database names
- `DATABASE`: Collection names
- `COLLECTION`: Documents
- `DOCUMENT`: Single document

See [Enriched Items Structure](#enriched-items-structure) for details.

### Query Parameters

| Variable | Type | Description | Default |
|----------|------|-------------|---------|
| `filter` | String | MongoDB query filter (JSON) | `""` (empty string) |
| `projection` | String | Field projection (JSON) | `""` (empty string) |
| `sortBy` | String | Sort specification (JSON) | `""` (empty string) |

### Usage Example
```html
<h1>
  {% if resourceType == 'ROOT' %}
    Databases
  {% elseif resourceType == 'DATABASE' %}
    Collections in {{ db }}
  {% elseif resourceType == 'COLLECTION' %}
    Documents in {{ db }}.{{ coll }}
  {% elseif resourceType == 'DOCUMENT' %}
    Document in {{ db }}.{{ coll }}
  {% endif %}
</h1>

{% if filter %}
  <p>Filtered by: <code>{{ filter }}</code></p>
{% endif %}
```

---

## Pagination Context

Variables for paginated results.

| Variable | Type | Description | Example |
|----------|------|-------------|---------|
| `page` | int | Current page number (1-indexed) | `1` |
| `pageSize` | int | Number of items per page | `25` |
| `totalPages` | int | Total number of pages | `10` |
| `totalItems` | long | Total item count | `237` |

### Set By
[MongoHtmlResponseHandler.addPaginationContext()](../core/src/main/java/org/facet/html/handlers/MongoHtmlResponseHandler.java)

### Usage Example
```html
<div class="pagination">
  <p>Page {{ page }} of {{ totalPages }} ({{ totalItems }} total items)</p>

  {% if page > 1 %}
    <a href="?page={{ page - 1 }}&pageSize={{ pageSize }}">Previous</a>
  {% endif %}

  {% if page < totalPages %}
    <a href="?page={{ page + 1 }}&pageSize={{ pageSize }}">Next</a>
  {% endif %}
</div>
```

---

## Permission Context

Boolean flags indicating what operations the authenticated user can perform.

| Variable | Type | Description |
|----------|------|-------------|
| `canCreateDatabases` | Boolean | Can create new databases |
| `canCreateCollections` | Boolean | Can create new collections |
| `canCreateDocuments` | Boolean | Can create documents in current collection |
| `canDeleteDatabase` | Boolean | Can delete current database |
| `canDeleteCollection` | Boolean | Can delete current collection |

### Set By
[MongoHtmlResponseHandler.resolveMountContext()](../core/src/main/java/org/facet/html/handlers/MongoHtmlResponseHandler.java)

### Usage Example
```html
{% if canCreateDocuments %}
  <button onclick="showCreateDialog()">Create Document</button>
{% endif %}

{% if canDeleteCollection %}
  <button onclick="deleteCollection()">Delete Collection</button>
{% endif %}
```

---

## Multi-Tenant Context

Variables for multi-tenant deployments using parametric mounts.

| Variable | Type | Description | Example |
|----------|------|-------------|---------|
| `tenantId` | String | Tenant identifier (JSON string or "null") | `"acme"`, `"null"` |
| `isMultiTenant` | Boolean | Whether this is a multi-tenant request | `true`, `false` |
| `hostParams` | String | Hostname parameters as JSON | `{"host":"acme.example.com","host[0]":"acme"}` |

### Set By
[MongoHtmlResponseHandler.addTenantContext()](../core/src/main/java/org/facet/html/handlers/MongoHtmlResponseHandler.java)

### Usage Example
```html
{% if isMultiTenant %}
  <div class="tenant-banner">
    <p>Tenant: {{ tenantId }}</p>
  </div>
{% endif %}
```

---

## Generic JSON Service Context

For non-MongoDB JSON services (using [JsonHtmlResponseHandler](../core/src/main/java/org/facet/html/handlers/JsonHtmlResponseHandler.java)).

| Variable | Type | Description |
|----------|------|-------------|
| `path` | String | Request path |
| `data` | String | Raw JSON response |
| *(dynamic)* | Various | All keys from JSON response merged into context |

### Usage Example
```html
<!-- For a service returning: {"status": "ok", "count": 42} -->
<p>Status: {{ status }}</p>
<p>Count: {{ count }}</p>

<!-- Raw JSON access -->
<pre>{{ data }}</pre>
```

---

## Enriched Items Structure

The `items` list contains enriched objects with additional metadata for navigation and rendering.

### For Database/Collection Names (Simple Strings)

```javascript
{
  "value": "products",    // The database or collection name
  "isString": true        // Flag indicating this is a simple string value
}
```

### For Documents (BSON Objects)

```javascript
{
  "data": BsonDocument,   // Full BSON document (accessible as object in templates)
  "isString": false,      // Flag indicating this is a document
  "_id": {                // ID metadata for URL generation
    "value": "507f1f77bcf86cd799439011",  // String representation of _id
    "type": null,         // ID type: null (ObjectId), "string", "number", etc.
    "needsParam": false   // Whether id_type query param is needed in URLs
  }
}
```

### ID Type Detection

The `_id.type` field helps generate correct URLs:
- `null`: MongoDB ObjectId (no query param needed)
- `"string"`: String _id (needs `?id_type=string`)
- `"number"`: Numeric _id (needs `?id_type=number`)
- etc.

### Usage Example
```html
{% for item in items %}
  {% if item.isString %}
    <!-- Database or collection name -->
    <li><a href="{{ path }}/{{ item.value }}">{{ item.value }}</a></li>
  {% else %}
    <!-- Document -->
    <li>
      <a href="{{ path }}/{{ item._id.value }}{% if item._id.needsParam and resourceType == 'COLLECTION' %}?id_type={{ item._id.type }}{% endif %}">
        {{ item._id.value }}
      </a>
      <!-- Access document fields -->
      <p>{{ item.data.name }}</p>
    </li>
  {% endif %}
{% endfor %}
```

---

## Common Patterns

### Building Navigation Links

```html
<!-- Breadcrumb navigation -->
{% set prefix = mongoPrefix == '/' ? '' : mongoPrefix %}
{% set segments = mongoPath | split('/') %}

<nav>
  <a href="{{ prefix }}/?page=1&pageSize={{ pageSize }}">Home</a>
  {% set currentPath = prefix %}
  {% for segment in segments %}
    {% if segment != '' %}
      {% set currentPath = currentPath ~ '/' ~ segment %}
      / <a href="{{ currentPath }}?page=1&pageSize={{ pageSize }}">{{ segment }}</a>
    {% endif %}
  {% endfor %}
</nav>
```

### Conditional Rendering by Resource Type

```html
{% if resourceType == 'ROOT' %}
  <h1>Databases</h1>
  <!-- List databases -->
{% elseif resourceType == 'DATABASE' %}
  <h1>Collections in {{ db }}</h1>
  <!-- List collections -->
{% elseif resourceType == 'COLLECTION' %}
  <h1>Documents in {{ db }}.{{ coll }}</h1>
  <!-- List documents -->
{% elseif resourceType == 'DOCUMENT' %}
  <h1>Document {{ db }}.{{ coll }}</h1>
  <!-- Show single document -->
{% endif %}
```

### Pagination with Preserved Query Parameters

```html
<div class="pagination">
  {% set baseQuery = "pageSize=" ~ pageSize %}
  {% if filter %}
    {% set baseQuery = baseQuery ~ "&filter=" ~ (filter | urlencode) %}
  {% endif %}
  {% if projection %}
    {% set baseQuery = baseQuery ~ "&projection=" ~ (projection | urlencode) %}
  {% endif %}
  {% if sortBy %}
    {% set baseQuery = baseQuery ~ "&sortBy=" ~ (sortBy | urlencode) %}
  {% endif %}

  {% if page > 1 %}
    <a href="?{{ baseQuery }}&page={{ page - 1 }}">Previous</a>
  {% endif %}

  <span>Page {{ page }} of {{ totalPages }}</span>

  {% if page < totalPages %}
    <a href="?{{ baseQuery }}&page={{ page + 1 }}">Next</a>
  {% endif %}
</div>
```

### Permission-Based UI

```html
<div class="actions">
  {% if resourceType == 'COLLECTION' and canCreateDocuments %}
    <button onclick="createDocument()">New Document</button>
  {% endif %}

  {% if resourceType == 'DATABASE' and canCreateCollections %}
    <button onclick="createCollection()">New Collection</button>
  {% endif %}

  {% if resourceType == 'ROOT' and canCreateDatabases %}
    <button onclick="createDatabase()">New Database</button>
  {% endif %}

  {% if resourceType == 'COLLECTION' and canDeleteCollection %}
    <button onclick="deleteCollection()" class="danger">Delete Collection</button>
  {% endif %}
</div>
```

### Debugging: Viewing All Context Variables

```html
<!-- Only in development -->
{% if username == "admin" %}
  <details>
    <summary>Debug: Template Context</summary>
    <pre>{{ _context | json_encode(constant('JSON_PRETTY_PRINT')) }}</pre>
  </details>
{% endif %}
```

---

## See Also

- [Developer's Guide](DEVELOPERS_GUIDE.md) - Complete guide to Facet development
- [Pebble Template Documentation](https://pebbletemplates.io/) - Template syntax reference
- [RESTHeart Documentation](https://restheart.org/docs/) - RESTHeart API reference
