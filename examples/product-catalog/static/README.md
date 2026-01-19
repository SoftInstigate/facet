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

Browsers automatically request `/favicon.ico` when you visit any page. Without this file:
1. Request hits RESTHeart with no matching resource
2. RESTHeart requires authentication → 401 response
3. Browser sees `WWW-Authenticate` header → shows login popup
4. User sees ugly browser auth dialog (not our custom login page)

By serving the favicon at `/favicon.ico`, we prevent this behavior.
