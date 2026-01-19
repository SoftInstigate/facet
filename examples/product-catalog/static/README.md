# Static Assets

This directory contains static assets for the Product Catalog example.

## Contents

- `favicon.ico` - Facet favicon (prevents browser 401 auth popup)

## Usage

Static assets are served via RESTHeart's `/static-resources` service:

- `/favicon.ico` → `static/favicon.ico` (at root to prevent browser auth popup)
- `/static/*` → `static/*` (all other assets)

## Adding More Assets

You can add more static files to this directory:

```
static/
├── favicon.ico
├── images/
│   └── logo.png
├── css/
│   └── custom.css
└── js/
    └── app.js
```

Then reference them in templates:

```html
<img src="/static/images/logo.png" alt="Logo">
<link rel="stylesheet" href="/static/css/custom.css">
<script src="/static/js/app.js"></script>
```

## Why the Favicon?

Browsers automatically request `/favicon.ico` when you visit any page. Without proper handling:
1. Request hits RESTHeart with no matching resource
2. Without ACL rules, RESTHeart requires authentication → 401 response
3. Browser sees `WWW-Authenticate` header → shows ugly login popup
4. User sees browser auth dialog instead of custom login page

**How Facet handles this:**
1. Base configuration (`shared/restheart-base.yml`) includes ACL rule allowing unauthenticated access to `/favicon.ico`, `/static/*`, etc.
2. This prevents 401 auth challenges for static assets
3. Missing files return 404 instead of triggering auth popup
4. Provided files are served normally via `/static-resources`

This two-layer approach ensures examples work smoothly even if static files are missing.
