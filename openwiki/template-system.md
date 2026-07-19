---
type: Reference
title: Template System
description: Facet's template resolution algorithm, naming conventions (list.html/view.html), hierarchical fallback, template context variables, Pebble custom filters, and error template support.
tags: [templates, pebble, resolution, naming, context, filters]
resource: core/src/main/java/org/facet/templates/PathBasedTemplateResolver.java
---

# Template System

Facet uses [Pebble](https://pebbletemplates.io) (a Jinja2/Twig-like template engine) to render HTML. Templates are resolved by **path convention**: the URL structure directly maps to the template directory structure. Template resolution is invoked by the [interceptor pipeline](architecture.md) described in the architecture overview.

## Resolution Algorithm

`PathBasedTemplateResolver` implements two resolution strategies:

### Full Page Resolution (`resolve()`)

Templates are looked up from most-specific to least-specific, with explicit action names tried before generic fallbacks.

**Collection requests** (e.g., `GET /mydb/products`):

```
1. templates/mydb/products/list.html     ← explicit collection view
2. templates/mydb/products/index.html    ← unified fallback
3. templates/mydb/list.html              ← parent collection
4. templates/mydb/index.html             ← parent fallback
5. templates/list.html                   ← global collection
6. templates/index.html                  ← global fallback
7. (no template) → JSON passthrough
```

**Document requests** (e.g., `GET /mydb/products/123`):

```
1. templates/mydb/products/123/view.html ← document-specific override
2. templates/mydb/products/view.html     ← explicit document view
3. templates/mydb/products/index.html    ← unified fallback
4. templates/mydb/view.html              ← parent document
5. templates/mydb/index.html             ← parent fallback
6. templates/view.html                   ← global document
7. templates/index.html                  ← global fallback
8. (no template) → JSON passthrough
```

### Fragment Resolution (`resolveFragment()`)

HTMX fragment requests (see [HTMX Integration](htmx.md)) use a strict 2-level lookup (no hierarchical walk):

```
1. templates/{resource-path}/_fragments/{target-id}.html
2. templates/_fragments/{target-id}.html
3. (not found) → 500 error (strict mode)
```

The `HX-Target` header value (with `#` stripped) determines the fragment filename.

Source: [`core/src/main/java/org/facet/templates/PathBasedTemplateResolver.java`](../core/src/main/java/org/facet/templates/PathBasedTemplateResolver.java)

## Naming Conventions

| Template | Use Case | Notes |
|----------|----------|-------|
| `list.html` | Collection views (multiple items) | Recommended — clean, no conditional logic |
| `view.html` | Document views (single item) | Recommended — clear intent |
| `index.html` | Unified fallback | Optional — use when list/view can share logic |
| `_fragments/{id}.html` | HTMX partials | Strict resolution, no hierarchy |
| `errors/{code}.html` | Per-status error pages | e.g., `errors/404.html` |
| `error.html` | Generic error fallback | Used when no status-specific template exists |
| `layout.html` | Base layout | Inherited via `{% extends "layout" %}` |

**Why explicit templates?** `list.html` and `view.html` eliminate the need for conditional logic in templates (checking `requestType` or `documents.length`). The filename declares intent.

**Template names are extensionless** in resolver calls — Pebble adds `.html` at render time.

## Context Variables

Templates receive context from `TemplateContextBuilder` and response handlers.

### Authentication

| Variable | Type | Description |
|----------|------|-------------|
| `isAuthenticated` | `boolean` | `true` if user is logged in |
| `username` | `String` | Current user name (null if not authenticated) |
| `roles` | `Set<String>` | User roles (null if not authenticated) |
| `requestMethod` | `String` | HTTP method: `GET`, `POST`, `PATCH`, `DELETE`, `PUT`, `HEAD`, `OPTIONS` |

### Resource Context

| Variable | Type | Description |
|----------|------|-------------|
| `path` | `String` | Full request path with mount prefix |
| `mongoPath` | `String` | MongoDB resource path (prefix stripped) |
| `mongoPrefix` | `String` | MongoDB mount prefix (e.g., `/api` or `/`) |
| `database` | `String` | Database name |
| `collection` | `String` | Collection name |
| `requestType` | `TYPE` | Enum: `ROOT`, `DB`, `COLLECTION`, `DOCUMENT` |

### MongoDB Data

| Variable | Type | Description |
|----------|------|-------------|
| `items` | `List<Map>` | Document objects with `data` (BSON as JSON) and metadata |
| `json` | `String` | JSON string of the full response |
| `page` | `int` | Current page number |
| `pagesize` | `int` | Items per page |
| `totalPages` | `int` | Total page count |
| `totalDocuments` | `long` | Total document count |
| `filter` | `String` | MongoDB filter query parameter |
| `sort` | `String` | Sort query parameter |
| `keys` | `String` | Keys (projection) query parameter |

### Mount Context

| Variable | Type | Description |
|----------|------|-------------|
| `mountedDatabase` | `String` | From parametric mongo-mounts |
| `mountedCollection` | `String` | From parametric mongo-mounts |
| `canCreateDatabases` | `boolean` | Permission flag |
| `canCreateCollections` | `boolean` | Permission flag |

### HTMX Context

| Variable | Type | Description |
|----------|------|-------------|
| `isHtmxRequest` | `boolean` | `true` if HTMX detected |
| `hxTarget` | `String` | The `HX-Target` value (without `#`) |

Source: [`core/src/main/java/org/facet/templates/TemplateContextBuilder.java`](../core/src/main/java/org/facet/templates/TemplateContextBuilder.java)

## Custom Pebble Filters

Facet registers four custom filters via `CustomPebbleExtension`:

| Filter | Usage | Example |
|--------|-------|---------|
| `stripTrailingSlash` | Removes trailing `/` (preserves root) | `{{ "/api/db/" \| stripTrailingSlash }}` → `"/api/db"` |
| `buildPath(segment)` | Appends path segment | `{{ "/api/db" \| buildPath("coll") }}` → `"/api/db/coll"` |
| `parentPath` | Returns parent path | `{{ "/api/db/coll/item" \| parentPath }}` → `"/api/db/coll"` |
| `toJson` | Serializes to JSON (pretty by default) | `{{ doc.data \| toJson }}` |

Source files:
- [`core/src/main/java/org/facet/templates/pebble/StripTrailingSlashFilter.java`](../core/src/main/java/org/facet/templates/pebble/StripTrailingSlashFilter.java)
- [`core/src/main/java/org/facet/templates/pebble/BuildPathFilter.java`](../core/src/main/java/org/facet/templates/pebble/BuildPathFilter.java)
- [`core/src/main/java/org/facet/templates/pebble/ParentPathFilter.java`](../core/src/main/java/org/facet/templates/pebble/ParentPathFilter.java)
- [`core/src/main/java/org/facet/templates/pebble/ToJsonFilter.java`](../core/src/main/java/org/facet/templates/pebble/ToJsonFilter.java)

## Error Templates

`HtmlResponseHelper.renderErrorPage()` resolves error templates with per-status-code support:

1. `templates/errors/{statusCode}.html` — status-specific page (e.g., `errors/404.html`)
2. `templates/error.html` — generic fallback

Added in v1.0.0 (commit `a5de99e`). An example `errors/404.html` is in the product-catalog example.

Source: [`core/src/main/java/org/facet/html/internal/HtmlResponseHelper.java`](../core/src/main/java/org/facet/html/internal/HtmlResponseHelper.java)

## Template Processing

`PebbleTemplateProcessor` wraps the Pebble engine. It is registered as a RESTHeart plugin (`pebble-template-processor`) and configured in `restheart.yml`:

```yaml
/pebble-template-processor:
  enabled: true
  use-file-loader: true
  cache-active: false           # true in production
  templates-path: /opt/restheart/templates
```

The processor supports:
- Global template context (shared across all renders)
- Locale-aware processing
- Template existence checking (`templateExists()`)
- Hot-reload when `cache-active: false` (edit templates, refresh browser)

Source: [`core/src/main/java/org/facet/templates/pebble/PebbleTemplateProcessor.java`](../core/src/main/java/org/facet/templates/pebble/PebbleTemplateProcessor.java)

## When Modifying Template Behavior

- **Change resolution order**: Edit `PathBasedTemplateResolver.resolve()` or `resolveFragment()`
- **Add global template variables**: Edit `TemplateContextBuilder.withAuthenticatedUser()` or the Mongo handler's `buildContext()`
- **Add a new Pebble filter**: Create a class extending `PebbleFilter`, register in `CustomPebbleExtension`
- **Change caching**: Edit `HtmlResponseHelper.setCachingHeaders()`
