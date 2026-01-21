# Facet Product Catalog - Guided Walkthrough

This guide walks you through a complete, working product catalog application to teach Facet concepts through real code.

> **🚀 Learning by Exploring**
>
> Instead of typing everything from scratch, you'll:
> 1. **Run the working example** in 2 minutes
> 2. **Explore the code** to understand how it works
> 3. **Make live changes** and see instant results
>
> All code is in [`examples/product-catalog/`](../examples/product-catalog/) - no need to recreate it!

## Table of Contents

1. [Get Started in 2 Minutes](#1-get-started-in-2-minutes)
2. [Level 1: Understanding Path-Based Templates](#2-level-1-understanding-path-based-templates)
3. [Level 2: Template Context Variables](#3-level-2-template-context-variables)
4. [Level 3: Hierarchical Template Resolution](#4-level-3-hierarchical-template-resolution)
5. [Level 4: MongoDB Query Parameters](#5-level-4-mongodb-query-parameters)
6. [Level 5: Pagination](#6-level-5-pagination)
7. [Level 6: HTMX Partial Updates](#7-level-6-htmx-partial-updates)
8. [Level 7: Authentication and JWT Cookies](#8-level-7-authentication-and-jwt-cookies)
9. [Level 8: Static Assets](#9-level-8-static-assets)
10. [Production Considerations](#10-production-considerations)

---

## 1. Get Started in 2 Minutes

### Run the Example

```bash
# Clone the repository
git clone https://github.com/SoftInstigate/facet.git
cd facet

# Build the plugin
mvn package -DskipTests

# Start everything with Docker Compose
cd examples
docker-compose up
```

**Wait for services to start** (few seconds).

**IMPORTANT - Set up authentication domain:**

JWT cookies require an RFC 6265 compliant domain. Add this to `/etc/hosts` (on Windows: `C:\Windows\System32\drivers\etc\hosts`):

```
127.0.0.1  local.getfacet.org
```

**Note:** `getfacet.org` is just an example - you can use any domain you prefer (e.g., `myapp.local`, `facet.test`). Just ensure the domain in `/etc/hosts`, `authCookieSetter` config, and your browser URL all match.

Then open:

👉 **http://local.getfacet.org:8080/shop/products**

**Critical:** You MUST use `local.getfacet.org` (not `localhost`) or login will fail silently. See the authentication section below for details.

You should see a styled product catalog with laptops, headphones, and other electronics.

### What Just Happened?

Docker Compose started three services:

1. **MongoDB** - Database with sample products (loaded from [init-data.js](../examples/product-catalog/init-data.js))
2. **RESTHeart** - REST API server with Facet plugin
3. **Template hot-reload** - Changes to templates reflect immediately

### Verify the Dual Interface

The same endpoint serves **both HTML and JSON**:

```bash
# Browser request → HTML
curl -u admin:secret http://local.getfacet.org:8080/shop/products -H "Accept: text/html"

# API request → JSON
curl -u admin:secret http://local.getfacet.org:8080/shop/products -H "Accept: application/json"
```

**Key concept**: Templates are opt-in. No template = JSON API unchanged.

---

## 2. Level 1: Understanding Path-Based Templates

### The Core Concept

**Request path = template path**

When you visit `/shop/products`, Facet looks for a template at `templates/shop/products/`.

### Explore the List Template

Open [examples/product-catalog/templates/shop/products/list.html](../examples/product-catalog/templates/shop/products/list.html)

**Key sections to notice:**

**Lines 1-12: Standard HTML with Pico CSS**
```html
<!doctype html>
<html lang="en" data-theme="light">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>{{ database | capitalize }} - Products</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@picocss/pico@2/css/pico.min.css">
    <script src="https://unpkg.com/htmx.org@2.0.8"></script>
</head>
```

Notice:
- Pico CSS is loaded from CDN for easy setup
- `{{ database }}` is a **template variable** from Facet's context

**Lines 25-29: Product iteration using context variables**
```html
{% for doc in documents %}
<article>
    <h3>{{ doc.name }}</h3>
    <p class="price">${{ doc.price }}</p>
</article>
{% endfor %}
```

The `documents` variable contains all products from MongoDB.

### Try It Yourself

1. **Edit the template** - Change line 27 from:
   ```html
   <h3>{{ doc.name }}</h3>
   ```
   to:
   ```html
   <h3>🛍️ {{ doc.name }}</h3>
   ```

2. **Refresh your browser** - See the emoji appear instantly (hot reload enabled)

3. **Revert the change** - Remove the emoji

### Template Naming Convention

Facet uses action-aware resolution:

- **Collection requests** → looks for `list.html` first, then `index.html`
- **Document requests** → looks for `view.html` first, then `index.html`

This is why our file is named `list.html` not `index.html`.

---

## 3. Level 2: Template Context Variables

### What Variables Are Available?

Facet automatically provides rich context variables to every template.

### Explore the Context

Open [examples/product-catalog/templates/shop/products/list.html](../examples/product-catalog/templates/shop/products/list.html) and look at **lines 20-23**:

```html
<header>
    <h1>{{ database | capitalize }} - Products Catalog</h1>
    <p>Showing {{ documents | length }} products</p>
</header>
```

**Variables used here:**
- `database` - Database name from URL path (`"shop"`)
- `documents` - Array of product documents from MongoDB
- `| capitalize` - Pebble filter (built-in)
- `| length` - Pebble filter (built-in)

### Available Context Variables

Open [docs/TEMPLATE_CONTEXT_REFERENCE.md](TEMPLATE_CONTEXT_REFERENCE.md) to see all variables.

**Most commonly used:**

| Variable | Description | Example |
|----------|-------------|---------|
| `documents` | Array of MongoDB documents | `{% for doc in documents %}` |
| `database` | Database name | `"shop"` |
| `collection` | Collection name | `"products"` |
| `path` | Full request path | `"/shop/products"` |
| `page` | Current page number | `1` |
| `pagesize` | Items per page | `25` |
| `totalPages` | Total page count | `4` |

### Try It Yourself

**Add a debug section** to see all variables:

1. Open [examples/product-catalog/templates/shop/products/list.html](../examples/product-catalog/templates/shop/products/list.html)

2. Add this **before the closing `</main>`** tag (around line 50):

```html
<details>
    <summary>Debug: Context Variables</summary>
    <ul>
        <li><strong>path:</strong> {{ path }}</li>
        <li><strong>database:</strong> {{ database }}</li>
        <li><strong>collection:</strong> {{ collection }}</li>
        <li><strong>requestType:</strong> {{ requestType }}</li>
        <li><strong>page:</strong> {{ page }}</li>
        <li><strong>pagesize:</strong> {{ pagesize }}</li>
        <li><strong>totalPages:</strong> {{ totalPages }}</li>
        <li><strong>totalDocuments:</strong> {{ totalDocuments }}</li>
    </ul>
</details>
```

3. **Refresh** and expand the "Debug: Context Variables" section

4. **Remove** this debug section when done exploring

---

## 4. Level 3: Hierarchical Template Resolution

### How Facet Finds Templates

When you request a URL, Facet walks up the directory tree looking for templates.

### Example: Document Detail Page

Visit: **http://local.getfacet.org:8080/shop/products/{any-product-id}**

Click on any product from the list to see its detail page.

### Explore the View Template

Open [examples/product-catalog/templates/shop/products/view.html](../examples/product-catalog/templates/shop/products/view.html)

**Key sections:**

**Lines 16-18: Single document access**
```html
{% if documents is not empty %}
    {% set product = documents[0] %}
    <h1>{{ product.name }}</h1>
```

For document requests, `documents` contains a single item (the requested document).

**Lines 45-47: Navigation back to list**
```html
<a href="{{ path | stripTrailingSlash | parentPath }}" role="button" class="secondary">
    ← Back to Products
</a>
```

Uses custom Facet filters: `stripTrailingSlash` and `parentPath`

### Template Resolution Algorithm

When requesting `/shop/products/65abc123...`:

**Facet looks for templates in this order:**

1. `templates/shop/products/65abc123.../view.html` ❌ (document-specific, doesn't exist)
2. `templates/shop/products/view.html` ✅ **FOUND!**
3. `templates/shop/products/index.html` (fallback if view.html missing)
4. `templates/shop/index.html` (parent directory fallback)
5. `templates/index.html` (root fallback)
6. **No template found** → return JSON (API unchanged)

This is **hierarchical resolution** - walks up the tree until it finds a template.

### Try It Yourself

**Test the fallback behavior:**

1. **Rename the view template**:
   ```bash
   cd examples/product-catalog
   mv templates/shop/products/view.html templates/shop/products/view.html.backup
   ```

2. **Visit a product detail page** - You'll now see `list.html` used (fallback to index.html equivalent)

3. **Restore the template**:
   ```bash
   mv templates/shop/products/view.html.backup templates/shop/products/view.html
   ```

---

## 5. Level 4: MongoDB Query Parameters

### RESTHeart Query Support

RESTHeart provides powerful MongoDB query parameters that Facet templates can use.

### Explore Sorting

Look at the sort links in [templates/shop/products/list.html](../examples/product-catalog/templates/shop/products/list.html) **lines 32-38**:

```html
<nav>
    <a href="?sort_by=name" class="secondary">Name</a>
    <a href="?sort_by=price" class="secondary">Price</a>
    <a href="?sort_by=category" class="secondary">Category</a>
</nav>
```

**Visit these URLs to see sorting in action:**
- http://local.getfacet.org:8080/shop/products?sort_by=name
- http://local.getfacet.org:8080/shop/products?sort_by=price
- http://local.getfacet.org:8080/shop/products?sort_by=-price (descending)

### MongoDB Query Parameters

RESTHeart supports these query parameters (all available as context variables):

| Parameter | Description | Example |
|-----------|-------------|---------|
| `filter` | MongoDB query in JSON | `?filter={"category":"Electronics"}` |
| `sort` | Sort specification | `?sort={"price":1}` (ascending) |
| `keys` | Field projection | `?keys={"name":1,"price":1}` |
| `page` | Page number | `?page=2` |
| `pagesize` | Items per page | `?pagesize=10` |

### Try It Yourself

**Test filtering via URL**:

Visit: http://local.getfacet.org:8080/shop/products?filter={"category":"Audio"}

Only audio products (headphones, earbuds) should appear.

**Test combination**:

Visit: http://local.getfacet.org:8080/shop/products?filter={"price":{"$lt":100}}&sort={"price":1}

Products under $100, sorted by price ascending.

---

## 6. Level 5: Pagination

### Automatic Pagination

RESTHeart automatically paginates results. Facet provides context variables for building pagination UI.

### Explore Pagination Controls

Look at [templates/shop/products/list.html](../examples/product-catalog/templates/shop/products/list.html) **lines 53-68**:

```html
{% if totalPages > 1 %}
<nav aria-label="Pagination">
    {% if page > 1 %}
        <a href="?page={{ page - 1 }}">← Previous</a>
    {% endif %}

    <span>Page {{ page }} of {{ totalPages }}</span>

    {% if page < totalPages %}
        <a href="?page={{ page + 1 }}">Next →</a>
    {% endif %}
</nav>
{% endif %}
```

### Pagination Context Variables

- `page` - Current page (1-indexed)
- `pagesize` - Items per page (default: 100)
- `totalPages` - Total number of pages
- `totalDocuments` - Total count of documents

### Try It Yourself

**Test pagination**:

1. **Change page size**: http://local.getfacet.org:8080/shop/products?pagesize=3
2. **Navigate pages**: Use Previous/Next links
3. **Direct page access**: http://local.getfacet.org:8080/shop/products?page=2&pagesize=5

The example has ~10 products, so you'll see multiple pages when pagesize is small.

---

## 7. Level 6: HTMX Partial Updates

### HTMX for Dynamic Updates

HTMX enables partial page updates without full reloads or writing JavaScript.

### Explore HTMX Integration

Look at [templates/shop/products/list.html](../examples/product-catalog/templates/shop/products/list.html) **lines 34-37**:

```html
<a href="?sort_by=name"
   hx-get="?sort_by=name"
   hx-target="#product-list"
   hx-swap="innerHTML">
   Name
</a>
```

**HTMX attributes explained:**
- `hx-get` - Make GET request to this URL
- `hx-target` - Update this element's content
- `hx-swap` - How to swap content (innerHTML, outerHTML, etc.)

### How HTMX Fragment Resolution Works

**When HTMX makes a request:**

1. HTMX sends headers: `HX-Request: true` and `HX-Target: product-list`
2. Facet detects HTMX request via `HtmxRequestDetector`
3. Template resolver looks for fragment template:
   - `templates/shop/products/_fragments/product-list.html` (resource-specific)
   - `templates/_fragments/product-list.html` (root fallback)
4. **Strict mode**: If fragment not found → 500 error (design decision to surface errors early)

### Explore the Fragment Template

Open [examples/product-catalog/templates/shop/products/_fragments/product-list.html](../examples/product-catalog/templates/shop/products/_fragments/product-list.html)

This is the **partial template** returned for HTMX requests.

**Notice:**
- No `<html>`, `<head>`, or `<body>` tags (just the fragment)
- Wraps content in `<div id="product-list">` (the target element)
- Contains same product loop as full page

### Try It Yourself

1. **Open browser DevTools** (Network tab)
2. **Click a sort link** (Name, Price, Category)
3. **Notice in Network tab**:
   - Only partial HTML returned (not full page)
   - Page doesn't flash/reload
   - URL updates in address bar
4. **View the response** - It's just the `#product-list` div content

**Test fragment template changes:**

1. Edit [templates/shop/products/_fragments/product-list.html](../examples/product-catalog/templates/shop/products/_fragments/product-list.html)
2. Add an emoji to line 8: `<h3>🎯 {{ doc.name }}</h3>`
3. Click a sort link (HTMX update) → see emoji
4. Full page refresh → emoji also appears

Both full page and HTMX partial now show the same updated HTML.

---

## 8. Level 7: Authentication and JWT Cookies

### Why the Domain Setup Matters

You may have noticed we're using `local.getfacet.org` instead of `localhost`. This is required for JWT cookie authentication.

### The Problem with localhost

According to **RFC 6265** (HTTP cookie specification), cookies set for `localhost` have inconsistent behavior and often fail silently in browsers. This causes login to appear successful but actually fail - creating an infinite redirect loop.

### The Solution

The domain must match in **three places**:

1. **`/etc/hosts` entry:** `127.0.0.1  local.getfacet.org`
2. **`authCookieSetter` config:** `domain: getfacet.org` (parent domain)
3. **Browser URL:** `http://local.getfacet.org:8080`

**Note:** `getfacet.org` is just an example. You can use any domain (`myapp.local`, `facet.test`, etc.) as long as all three places match.

### Try It Yourself

**Test the authentication flow:**

1. Visit: http://local.getfacet.org:8080/login
2. Log in as `admin` / `secret`
3. Open DevTools → Application → Cookies
4. Inspect the `rh_auth` cookie (domain: `.getfacet.org`)

**See the failure with localhost:**

1. Visit: http://localhost:8080/login
2. Try to log in - appears to work
3. Check DevTools → no `rh_auth` cookie appears!
4. Infinite redirect back to login

This demonstrates why the proper domain is required.

**Full technical explanation:** See [Developer's Guide - JWT Cookie Authentication](DEVELOPERS_GUIDE.md#jwt-cookie-authentication-requires-proper-domain) for details on RFC 6265, alternative domains, and production setup.

---

## 9. Level 8: Static Assets

### Serving CSS, JS, and Images

Facet integrates with RESTHeart's static file serving.

### Explore Static Asset Configuration

Open [examples/product-catalog/restheart.yml](../examples/product-catalog/restheart.yml) **lines 32-38**:

```yaml
/static:
  enabled: true
  what: /static/
  where: /opt/restheart/static
  embedded-resources-prefix: null
  index-file: null
  etag-policy: IF_MATCH_POLICY
```

This maps `/static/*` URLs to the `/opt/restheart/static` directory.

### Static Assets in the Example

Check [examples/product-catalog/static/](../examples/product-catalog/static/):

```
static/
└── images/
    └── placeholder-*.svg # Product images
```

Note: Pico CSS is loaded from CDN (https://cdn.jsdelivr.net/npm/@picocss/pico@2/css/pico.min.css) for easier setup without local dependencies.

### Referenced in Templates

Look at [templates/layout.html](../examples/product-catalog/templates/layout.html) **line 9**:

```html
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@picocss/pico@2/css/pico.min.css">
```

And **lines 30-35** in the product template for images:

```html
<img src="{{ doc.imageUrl }}" alt="{{ doc.name }}">
```

### Try It Yourself

**Test static file serving:**

1. Visit: http://local.getfacet.org:8080/static/images/ (see images if any are added)
2. Visit: http://local.getfacet.org:8080/static/images/placeholder-laptop.svg (see image)

**Add a custom style:**

1. Edit [examples/product-catalog/static/custom.css](../examples/product-catalog/static/custom.css)
2. Add at the end:
   ```css
   article h3 {
       color: #ff6b6b;
       font-weight: bold;
   }
   ```
3. Refresh the product list → product names now red

---

## 10. Production Considerations

### What Changes for Production?

The example is configured for development. Here's what to adjust for production.

### Configuration Changes

Compare development vs production settings:

**Development** ([examples/product-catalog/restheart.yml](../examples/product-catalog/restheart.yml)):
```yaml
/html-response-interceptor:
  enabled: true
  response-caching: false  # Hot reload
  max-age: 5               # Short cache
```

**Production**:
```yaml
/html-response-interceptor:
  enabled: true
  response-caching: true   # Enable ETag caching
  max-age: 300             # 5 minutes
```

### Security Considerations

**For production, review:**

1. **Authentication** - See [examples/product-catalog/users.yml](../examples/product-catalog/users.yml) for user setup
2. **CORS settings** - Restrict `allow-origin` to your domain
3. **MongoDB connection** - Use environment variables for credentials
4. **HTTPS** - Enable SSL/TLS in RESTHeart configuration

### Performance Optimizations

**ETag Caching:**
- Facet generates ETags from rendered HTML's hash
- Browsers send `If-None-Match` header
- 304 Not Modified responses for unchanged content

**MongoDB Indexes:**
```bash
# Add indexes for frequently queried fields
db.products.createIndex({ category: 1 })
db.products.createIndex({ price: 1 })
db.products.createIndex({ name: "text" })
```

### Monitoring

**Check RESTHeart logs:**
```bash
docker-compose logs -f restheart
```

**Health check endpoint:**
http://local.getfacet.org:8080/_ping

---

## Summary: Key Concepts Learned

Through this walkthrough, you explored:

1. **Path-Based Templates** - Template location mirrors API URL structure
2. **Template Context** - Rich variables automatically provided (documents, pagination, etc.)
3. **Hierarchical Resolution** - Template fallback up the directory tree
4. **MongoDB Queries** - Filter, sort, pagination via URL parameters
5. **HTMX Fragments** - Partial updates without JavaScript
6. **Static Assets** - CSS, JS, images served alongside templates
7. **Dual Interface** - Same endpoint serves HTML (browsers) and JSON (APIs)

## Next Steps

### Experiment Further

Try these modifications to deepen your understanding:

1. **Add a new field** - Edit [init-data.js](../examples/product-catalog/init-data.js), add `description` field, show it in templates
2. **Create a detail fragment** - Make the view page support HTMX updates
3. **Add filtering UI** - Create category filter buttons using HTMX
4. **Build a search bar** - Use MongoDB text search with `$text` operator
5. **Customize styling** - Modify [static/custom.css](../examples/product-catalog/static/custom.css)

### Dive Deeper

- **[Developer's Guide](DEVELOPERS_GUIDE.md)** - Complete Facet architecture and APIs
- **[Template Context Reference](TEMPLATE_CONTEXT_REFERENCE.md)** - All available context variables
- **[RESTHeart Documentation](https://restheart.org/docs/)** - MongoDB API features
- **[Pebble Templates](https://pebbletemplates.io/)** - Template syntax reference
- **[HTMX Documentation](https://htmx.org/docs/)** - Advanced HTMX patterns

### Build Your Own

Start a new project using the example as a template:

```bash
# Copy the example
cp -r examples/product-catalog my-project
cd my-project

# Customize for your data model
# 1. Edit init-data.js with your data
# 2. Update templates with your fields
# 3. Modify restheart.yml for your paths
# 4. Start building!
```

---

**Happy building with Facet!** 🚀
