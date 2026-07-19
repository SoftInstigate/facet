---
type: Reference
title: HTMX Integration
description: How Facet detects HTMX requests, resolves fragment templates, provides server-side HTMX response headers, handles SSE bypass, and supports mutation patterns with partial page updates.
tags: [htmx, fragments, partial-updates, progressive-enhancement, sse]
resource: core/src/main/java/org/facet/html/internal/HtmxRequestDetector.java
---

# HTMX Integration

Facet provides first-class [HTMX](https://htmx.org) support for progressive enhancement: start with server-rendered HTML, add partial page updates where needed, and keep pages fast and SEO-friendly. HTMX integration is part of the [interceptor pipeline](architecture.md) — the same response interceptor handles both full pages and fragments.

## Request Detection

`HtmxRequestDetector` identifies HTMX requests by checking HTTP headers:

| Header | Meaning |
|--------|---------|
| `HX-Request: true` | Standard HTMX request marker |
| `HX-Target` | Target element ID for fragment swap |
| `HX-Trigger` | Element that triggered the request |
| `HX-Trigger-Name` | Name of the triggering element |
| `HX-Prompt` | User prompt value |
| `HX-Boosted` | Boosted link/form flag |

A request is considered HTMX when `HX-Request: true` is set **and** the `Accept` header is `*/*` (HTMX's default). This is important because HTMX sends `Accept: */*` rather than `Accept: text/html`.

The `acceptsHtml()` method in `HtmlResponseHelper` handles both cases:
- Standard browsers: `Accept: text/html`
- HTMX requests: `HX-Request: true` + `Accept: */*`

Source: [`core/src/main/java/org/facet/html/internal/HtmxRequestDetector.java`](../core/src/main/java/org/facet/html/internal/HtmxRequestDetector.java)

## Fragment Resolution

When an HTMX request includes `HX-Target`, Facet resolves a **fragment template** instead of a full page (see [Template System](template-system.md) for the full resolution algorithm):

```
GET /mydb/products
HX-Request: true
HX-Target: #product-list

Resolution:
1. templates/mydb/products/_fragments/product-list.html
2. templates/_fragments/product-list.html
3. → 500 error (strict mode)
```

**Strict mode** is a deliberate design decision: a missing fragment template returns a 500 error rather than silently falling back. This surfaces configuration errors early during development.

Fragment templates are organized in `_fragments/` subdirectories to keep them visually distinct from page templates. The `#` prefix is stripped from the `HX-Target` value before lookup.

Source: [`PathBasedTemplateResolver.resolveFragment()`](../core/src/main/java/org/facet/templates/PathBasedTemplateResolver.java)

## Server-Side HTMX Response Headers

`HtmxResponseHelper` provides static methods for setting HTMX response headers from custom services or interceptors:

### Client-Side Events

| Method | Header | Purpose |
|--------|--------|---------|
| `triggerEvent(event)` | `HX-Trigger` | Trigger a client-side event after swap |
| `triggerEventAfterSwap(event)` | `HX-Trigger-After-Swap` | Trigger after DOM swap |
| `triggerEventAfterSettle(event)` | `HX-Trigger-After-Settle` | Trigger after settle |

### Swap Control

| Method | Header | Purpose |
|--------|--------|---------|
| `reswap(swapStyle)` | `HX-Reswap` | Override swap strategy (innerHTML, outerHTML, etc.) |
| `retarget(target)` | `HX-Retarget` | Change the target element |

### Navigation

| Method | Header | Purpose |
|--------|--------|---------|
| `pushUrl(url)` | `HX-Push-Url` | Push URL to browser history |
| `replaceUrl(url)` | `HX-Replace-Url` | Replace current URL |
| `redirect(url)` | `HX-Redirect` | Full page redirect |
| `refresh()` | `HX-Refresh` | Full page refresh |

Source: [`core/src/main/java/org/facet/html/internal/HtmxResponseHelper.java`](../core/src/main/java/org/facet/html/internal/HtmxResponseHelper.java)

## Mutation Patterns

Facet supports three patterns for handling form submissions and API mutations:

### 1. HTMX Fragment Swap (Recommended)

Use HTMX forms with `hx-post` / `hx-patch` / `hx-delete` and target a fragment:

```html
<form hx-patch="/shop/products/{{ item.data._id }}"
      hx-target="#product-form"
      hx-swap="outerHTML">
  <input name="name" value="{{ item.data.name }}">
  <button>Save</button>
</form>
```

The fragment template re-renders the updated state. No redirect needed — HTMX swaps the DOM directly.

### 2. Post-Redirect-Get

Standard form submission with `302` redirect:

```html
<form method="POST" action="/shop/products">
  <input name="name">
  <button>Create</button>
</form>
```

After POST, redirect to the collection page. Works without JavaScript.

### 3. Inline Response with `requestMethod`

Templates can check `requestMethod` for conditional rendering:

```html
{% if requestMethod == "POST" %}
  <div class="success">Product created!</div>
{% endif %}
```

The `requestMethod` variable is always available in the template context (set by `TemplateContextBuilder`).

Source: CHANGELOG.md — "Handling Mutations" section was documented in commit `7c9283c`.

## SSE Bypass

Server-Sent Events requests (`Accept: text/event-stream`) are explicitly excluded from Facet's HTML interception. This ensures RESTHeart's SSE infrastructure handles the response unchanged. Added in commit `8d251a2` to fix issue #8.

```java
// In HtmlResponseInterceptor.resolve():
if (isEventStreamRequest(request.getHeaders())) {
    return false; // pass through
}
```

Source: [`HtmlResponseInterceptor.java`](../core/src/main/java/org/facet/html/HtmlResponseInterceptor.java)

## Quickstart Template Example

The root `/templates` directory includes a working quickstart with HTMX:

```
templates/
├── index.html              ← home page (global fallback)
├── layout.html             ← base layout (Pico CSS + HTMX CDN)
├── login/index.html        ← login form
└── mydb/products/list.html ← product collection view
```

`layout.html` includes HTMX from CDN (`htmx.org@2.0.8`) and Pico CSS (`@picocss/pico@2`).

## Product Catalog Example

The `examples/product-catalog` demonstrates full HTMX-powered CRUD:

```
templates/
├── _fragments/
│   ├── product-form.html   ← edit form (HTMX fragment)
│   ├── product-list.html   ← product list (HTMX fragment)
│   └── product-new.html    ← create form (HTMX fragment)
├── shop/products/
│   ├── list.html           ← collection page
│   └── view.html           ← document page
├── errors/404.html         ← custom 404 page
├── error.html              ← generic error fallback
└── layout.html             ← shared layout
```

See [`examples/product-catalog/README.md`](../examples/product-catalog/README.md) for the full walkthrough.

## Known Limitation: HX-Reselect

The `HX-Reselect` response header is not implemented. This minor gap means server-side selection of which part of the response to swap is unavailable. Listed in CHANGELOG.md as future work.
