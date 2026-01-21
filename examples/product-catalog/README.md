# Product Catalog Example

A complete example demonstrating Facet's core features through a product catalog application.

## Features Demonstrated

- **Path-based template resolution** - Templates organized by URL structure
- **MongoDB data binding** - Direct display of MongoDB documents
- **Search and filtering** - MongoDB query parameters (`filter`, `sort`, `keys`)
- **Pagination** - Navigate large datasets with `page` and `pagesize`
- **HTMX partial updates** - Search and paginate without full page reloads
- **Authentication** - Role-based access control (admin vs viewer)
- **CRUD operations** - Create, read, update, delete products (admin only)
- **Template inheritance** - Shared layout and reusable fragments

## Quick Start

### Prerequisites

- Docker and Docker Compose installed
- Facet core plugin built: `mvn package` from repository root

### Running the Example

1. **Build the Facet plugin** (from repository root):
   ```bash
   cd /path/to/facet
   mvn package -DskipTests
   ```

2. **Start the example** (from the product-catalog directory):
   ```bash
   cd examples/product-catalog
   docker-compose up --build
   ```

   The `--build` flag ensures the Facet Docker image is built with the plugin included.

3. **Wait for services to start** (watch logs for "RESTHeart started" message):
   ```bash
   docker-compose logs -f facet
   ```

4. **Configure local domain for JWT authentication** (required for login to work):

   JWT cookies require an RFC 6265 compliant domain (cannot use `localhost`). Add this line to `/etc/hosts`:

   ```
   127.0.0.1  local.getfacet.org
   ```

   On Windows, edit `C:\Windows\System32\drivers\etc\hosts` instead.

5. **Access the application**:
   - Product Catalog: http://local.getfacet.org:8080/shop/products
   - Facet API: http://local.getfacet.org:8080/shop/products (with `Accept: application/json`)
   - Ping endpoint: http://local.getfacet.org:8080/ping

   **Important:** You MUST use `local.getfacet.org` (not `localhost`) for authentication to work. See the [Authentication Setup](#authentication-setup) section for details.

### User Accounts

The example includes two pre-configured users:

| Username | Password | Role    | Permissions                |
|----------|----------|---------|----------------------------|
| `admin`  | `secret` | admin   | Full access (CRUD)         |
| `viewer` | `viewer` | viewer  | Read-only access           |

**Login URL**: http://local.getfacet.org:8080/login

> **Why not localhost?** JWT cookies require an RFC 6265 compliant domain. Using `localhost` will cause login to silently fail. See [Authentication Setup](#authentication-setup) for details.

## What's Included

### Sample Data

The example automatically loads 10 sample products into the `shop.products` collection:
- Electronics (laptop, mouse, keyboard, monitor, etc.)
- Furniture (chair, desk, lamp)
- Appliances (coffee maker)

All products include:
- Name, description, price
- Category and stock level
- Tags for filtering
- Timestamps

### Templates

```
templates/
├── layout.html                      # Base layout with navigation
├── shop/
│   └── products/
│       ├── index.html              # Product list page
│       └── view.html               # Product detail page
└── _fragments/
    └── product-list.html           # Reusable product list (for HTMX)
```

### Configuration Files

- [restheart.yml](restheart.yml) - RESTHeart configuration with Facet settings
- [users.yml](users.yml) - File-based user authentication and ACL rules
- [init-data.js](init-data.js) - MongoDB initialization script with sample products
- [static/](static/) - Static assets (favicon, images, CSS, JS)
- [Dockerfile](Dockerfile) - Docker image with Facet plugin pre-installed
- [docker-compose.yml](docker-compose.yml) - Multi-container setup (RESTHeart + MongoDB)

## Template Resolution Examples

When you request different URLs, Facet resolves templates hierarchically:

| Request URL | Template Resolved | Fallback Order |
|-------------|-------------------|----------------|
| `GET /shop/products` | `shop/products/index.html` | → `shop/index.html` → `index.html` |
| `GET /shop/products/65abc...` | `shop/products/view.html` | → `shop/products/index.html` → ... |
| `GET /shop/products` (HTMX, target: `#product-list`) | `_fragments/product-list.html` | No fallback (strict mode) |

## Key Features Explained

### 1. Search and Filtering

The search form demonstrates MongoDB query building:

```html
<!-- Form builds filter parameter -->
<form method="GET" hx-get="/shop/products" hx-target="#product-list">
  <input id="searchText" placeholder="Search...">
  <select id="categorySelect">...</select>
  <input type="hidden" name="filter" id="filterInput">
</form>

<script>
// JavaScript builds MongoDB query
let filter = {};
if (searchText) filter.$text = { $search: searchText };
if (category) filter.category = category;
document.getElementById('filterInput').value = JSON.stringify(filter);
</script>
```

**Result**: `?filter={"category":"Electronics","price":{"$lte":100}}`

### 2. HTMX Partial Updates

Pagination links include HTMX attributes for partial updates:

```html
<a href="?page=2&pagesize=10"
   hx-get="/shop/products?page=2&pagesize=10"
   hx-target="#product-list">
    Next
</a>
```

**What happens**:
1. Click triggers HTMX request with `HX-Request: true` header
2. Facet detects HTMX and resolves `_fragments/product-list.html`
3. Only the product list div is replaced, keeping search form intact

### 3. Role-Based UI

Templates use `roles` context variable to show/hide features:

```html
{% if roles and 'admin' in roles %}
    <a href="/shop/products/new" class="button is-primary">Add Product</a>
{% endif %}
```

**Authentication Flow**:
1. Unauthenticated user → redirected to `/login`
2. Login sets `rh_auth` cookie with JWT token
3. Templates receive `username` and `roles` variables
4. RESTHeart ACL enforces write restrictions

### 4. Template Context Variables

All templates have access to these variables:

| Variable | Example Value | Description |
|----------|---------------|-------------|
| `path` | `/shop/products` | Full request path |
| `database` | `shop` | MongoDB database name |
| `collection` | `products` | MongoDB collection name |
| `items` | `[{data: {...}, _id: {...}}, ...]` | Array of documents |
| `page` | `2` | Current page number |
| `pagesize` | `10` | Items per page |
| `totalItems` | `50` | Total document count |
| `totalPages` | `5` | Total pages |
| `filter` | `{"category":"Electronics"}` | MongoDB filter (JSON string) |
| `sort` | `{"price":1}` | MongoDB sort (JSON string) |
| `username` | `admin` | Current user (null if not authenticated) |
| `roles` | `['admin']` | User roles array |

## Development Workflow

### Hot Reload Templates

Templates are loaded from the filesystem with caching disabled:

```yaml
/pebble-template-processor:
  use-file-loader: true
  cache-active: false
```

**Edit a template** → **Refresh browser** → See changes immediately!

### Viewing Logs

```bash
# Facet/RESTHeart logs
docker-compose logs -f facet

# MongoDB logs
docker-compose logs -f mongodb
```

### Testing the API

The same endpoints serve JSON for API clients:

```bash
# List products (JSON)
curl -u admin:secret http://localhost:8080/shop/products

# Get single product
curl -u admin:secret http://localhost:8080/shop/products/65abc123...

# Search with filter
curl -u admin:secret "http://localhost:8080/shop/products?filter=%7B%22category%22%3A%22Electronics%22%7D"

# Create product (requires authentication)
curl -u admin:secret -X POST http://localhost:8080/shop/products \
  -H "Content-Type: application/json" \
  -d '{"name":"New Product","price":99.99,"category":"Test","stock":10}'
```

### Stopping the Example

```bash
docker-compose down

# Remove data volume (reset to initial state)
docker-compose down -v
```

## Extending the Example

### Adding New Templates

Create new templates matching your URL structure:

```
templates/
└── shop/
    ├── products/
    │   ├── index.html          # /shop/products
    │   ├── view.html           # /shop/products/{id}
    │   └── categories/
    │       └── index.html      # /shop/products/categories
    └── orders/
        └── index.html          # /shop/orders
```

### Adding More Data

Edit `init-data.js` and restart services:

```javascript
db.products.insertMany([
  { name: "Your Product", price: 49.99, category: "New", stock: 100 }
]);
```

Or use the API:

```bash
curl -u admin:secret -X POST http://localhost:8080/shop/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Runtime Product","price":19.99,"category":"Dynamic","stock":5}'
```

### Customizing Authentication

Edit `users.yml` to add more users or change permissions:

```yaml
users:
  - userid: editor
    password: editor123
    roles: [editor]

permissions:
  - role: editor
    predicate: path-prefix[path=/shop/products]
    priority: 2
    mongo:
      readFilter: null
      writeFilter: null
```

## Next Steps

- **Add more features**: Shopping cart, checkout flow, reviews
- **Explore HTMX**: Add more interactive components without JavaScript
- **Customize styling**: Pico CSS provides semantic defaults - add custom CSS in `<style>` or create a custom theme
- **Add validation**: Form validation and error handling
- **Implement search**: Full-text search with MongoDB text indexes
- **Add images**: Product images with file upload

## Styling with Pico CSS

This example uses [Pico CSS](https://picocss.com/) - a minimal, semantic CSS framework similar to the approach used by [FastHTML](https://fastht.ml/). Pico automatically styles semantic HTML elements without requiring class names:

- Write `<nav><ul><li>` → Get a beautiful navigation bar
- Write `<article>` → Get styled cards
- Write `<button>` or `<a role="button">` → Get styled buttons
- Use `<mark>`, `<ins>`, `<del>`, `<kbd>` → Get semantic highlights

**Benefits**: Clean HTML, minimal custom CSS, better accessibility, smaller footprint (~10KB vs 200KB for utility frameworks).

To customize: Add your own CSS in the `<style>` block or override Pico's CSS variables.

## Resources

- [Facet Developer's Guide](../../docs/DEVELOPERS_GUIDE.md)
- [Template Context Reference](../../docs/TEMPLATE_CONTEXT_REFERENCE.md)
- [Product Catalog Tutorial](../../docs/TUTORIAL_PRODUCT_CATALOG.md)
- [RESTHeart Documentation](https://restheart.org/docs/)
- [Pebble Templates](https://pebbletemplates.io/)
- [HTMX Documentation](https://htmx.org/)
- [Pico CSS Documentation](https://picocss.com/)

## Authentication Setup

### Quick Setup

JWT authentication requires a proper domain (not `localhost`). Add this to `/etc/hosts`:

**macOS/Linux:**
```bash
echo "127.0.0.1  local.getfacet.org" | sudo tee -a /etc/hosts
```

**Windows (as Administrator):**
```
echo 127.0.0.1  local.getfacet.org >> C:\Windows\System32\drivers\etc\hosts
```

Then access the app at **http://local.getfacet.org:8080** (not `localhost`).

### Why This Is Required

Using `localhost` causes JWT cookies to fail silently due to RFC 6265 restrictions. The domain must match in three places:
1. `/etc/hosts` entry (e.g., `local.getfacet.org`)
2. `authCookieSetter` domain in [restheart.yml](restheart.yml) (e.g., `getfacet.org`)
3. Browser URL (e.g., `http://local.getfacet.org:8080`)

**Note:** `getfacet.org` is just an example. You can use any domain (`myapp.local`, `facet.test`, etc.) as long as all three places match.

**Full details:** See [Developer's Guide - JWT Cookie Authentication](../../docs/DEVELOPERS_GUIDE.md#jwt-cookie-authentication-requires-proper-domain)

## Troubleshooting

### Login doesn't work / infinite redirect

**Symptom:** Clicking login redirects you back to the login page, or you can't stay logged in.

**Cause:** You're accessing the app via `localhost` instead of the configured domain.

**Solution:**
1. Verify `/etc/hosts` contains `127.0.0.1  local.getfacet.org`
2. Access the app via http://local.getfacet.org:8080 (not `localhost`)
3. Clear browser cookies for `localhost` and `local.getfacet.org`
4. Try logging in again

### Services won't start

```bash
# Check if ports are in use
lsof -i :8080
lsof -i :27017

# View detailed logs
docker-compose logs
```

### Template not found

- Check template path matches URL structure
- Ensure [templates/](templates/) directory exists and is properly structured
- Check RESTHeart logs for template resolution attempts

### Authentication issues

**If login doesn't work, check these in order:**

1. **Are you using the correct domain?**
   - ✅ Use: http://local.getfacet.org:8080/login
   - ❌ Not: http://localhost:8080/login
   - See [Authentication Setup](#authentication-setup) for why this matters

2. **Is your hosts file configured?**
   ```bash
   # macOS/Linux
   cat /etc/hosts | grep getfacet

   # Should show: 127.0.0.1  local.getfacet.org
   ```

3. **Clear browser cookies:**
   - Open browser DevTools → Application → Cookies
   - Delete all cookies for `localhost` and `local.getfacet.org`
   - Try logging in again

4. **Verify users.yml syntax:**
   - Check [users.yml](users.yml) for typos
   - Ensure proper YAML indentation

5. **Test authentication with curl:**
   ```bash
   # This should work (basic auth)
   curl -u admin:secret http://local.getfacet.org:8080/shop/products
   ```

### Data not loading

- Check MongoDB logs: `docker-compose logs mongodb`
- Verify init-data.js syntax
- Manually insert data: `docker exec -it facet-product-catalog-mongo mongosh`
