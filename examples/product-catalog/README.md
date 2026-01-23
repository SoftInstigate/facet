# Product Catalog Example

A complete example demonstrating Facet's core features through a product catalog application.

## Features Demonstrated

- **Path-based template resolution** - Templates organized by URL structure
- **MongoDB data binding** - Direct display of MongoDB documents
- **Search and filtering** - MongoDB query parameters (`filter`, `sort`, `keys`)
- **Pagination** - Navigate large datasets with `page` and `pagesize`
- **HTMX partial updates** - Search and paginate without full page reloads
- **Authentication** - Role-based access control (admin vs viewer)
- **CRUD operations** - Full create, read, update, delete using HTMX fragments (see below)
- **Template inheritance** - Shared layout and reusable fragments

## CRUD Operations

The product catalog demonstrates full CRUD using HTMX fragments:

- **Create**: Click "+ Add Product" → HTMX loads [_fragments/product-new.html](templates/_fragments/product-new.html) fragment → Form submits via POST
- **Read**: Click product card → Navigates to `/products/{id}` with [view.html](templates/shop/products/view.html) template
- **Update**: Click "Edit Product" → HTMX loads [_fragments/product-form.html](templates/_fragments/product-form.html) fragment → Form submits via PATCH
- **Delete**: Click "Delete Product" → JavaScript `fetch()` with DELETE method

All operations use RESTHeart's native MongoDB REST API with standard HTTP methods (POST/GET/PATCH/DELETE) - no custom backend code needed.

### Why Fragments?

- **Keeps URLs clean and REST-compliant**: `/products/{id}` not `/products/{id}/edit`
- **No routing conflicts**: Document IDs never conflict with action names (e.g., a document with `_id: "edit"` works correctly as `/products/edit`)
- **Forms load instantly**: No page refresh, smooth user experience via HTMX
- **URLs remain shareable**: Use query parameters like `?mode=edit` for deep linking to specific UI states
- **Component reusability**: Same fragment can be loaded from list page or detail page

### The Pattern

This architectural approach provides:
1. **Clean REST URLs** - Resources, not actions: `/products/{id}` represents the resource
2. **HTTP methods for actions** - POST (create), GET (read), PATCH (update), DELETE (delete)
3. **Fragments for UI components** - Forms are reusable components loaded via HTMX
4. **Progressive enhancement** - Works with JavaScript (HTMX), can fallback to traditional POST/redirect
5. **No backend code** - RESTHeart handles all MongoDB operations directly

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

4. **Access the application**:
   - Product Catalog: http://localhost:8080/shop/products
   - Facet API: http://localhost:8080/shop/products (with `Accept: application/json`)
   - Ping endpoint: http://localhost:8080/ping

   **Authentication:** See the [Authentication Setup](#authentication-setup) section below for details on user credentials and permissions.

### User Accounts

The example includes two pre-configured users:

| Username | Password | Role    | Permissions                |
|----------|----------|---------|----------------------------|
| `admin`  | `secret` | admin   | Full access (CRUD)         |
| `viewer` | `viewer` | viewer  | Read-only access           |

**Login URL**: http://localhost:8080/login

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
│       ├── list.html               # Product list page (collection view)
│       └── view.html               # Product detail page (document view)
└── _fragments/
    └── product-list.html           # Reusable product list (for HTMX)
```

### Configuration Files

- [restheart.yml](restheart.yml) - RESTHeart configuration with Facet settings and ACL permissions
- [users.yml](users.yml) - File-based user credentials and role assignments
- [init-data.js](init-data.js) - MongoDB initialization script with sample products
- [static/](static/) - Static assets (favicon, images, CSS, JS)
- [Dockerfile](Dockerfile) - Docker image with Facet plugin pre-installed
- [docker-compose.yml](docker-compose.yml) - Multi-container setup (RESTHeart + MongoDB)

## Template Resolution Examples

When you request different URLs, Facet resolves templates hierarchically:

| Request URL | Template Resolved | Fallback Order |
|-------------|-------------------|----------------|
| `GET /shop/products` | `shop/products/list.html` | → `shop/products/index.html` → `shop/list.html` → `shop/index.html` → `list.html` → `index.html` |
| `GET /shop/products/65abc...` | `shop/products/view.html` | → `shop/products/index.html` → `shop/view.html` → ... |
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

**Authentication & Authorization Flow**:
1. Unauthenticated user → redirected to `/login`
2. Login validates credentials via `fileRealmAuthenticator` (users.yml)
3. On success, `jwtTokenManager` issues JWT token stored in `rh_auth` cookie
4. Templates receive `username` and `roles` variables from JWT
5. `fileAclAuthorizer` enforces role-based permissions (defined in restheart.yml)

See the [Authentication Setup](#authentication-setup) section for details.

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

## Authentication Setup

This example uses **file-based authentication** for simplicity. RESTHeart separates authentication (who you are) from authorization (what you can do):

### Authentication (fileRealmAuthenticator)

User credentials and role assignments are defined in [users.yml](users.yml):

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

The `fileRealmAuthenticator` validates credentials and assigns roles during login. Configured in [restheart.yml](restheart.yml):

```yaml
/fileRealmAuthenticator:
  enabled: true
  conf-file: /opt/restheart/etc/users.yml

/basicAuthMechanism:
  enabled: true
  authenticator: fileRealmAuthenticator
```

### Authorization (fileAclAuthorizer)

Role-based permissions (ACL) are defined in [restheart.yml](restheart.yml):

```yaml
/fileAclAuthorizer:
  enabled: true
  permissions:
    # Admin can do anything
    - role: admin
      predicate: path-prefix[path=/]
      priority: 0
      mongo:
        readFilter: null
        writeFilter: null

    # Viewer can only read
    - role: viewer
      predicate: path-prefix[path=/]
      priority: 1
      mongo:
        readFilter: null
        writeFilter: '{"_id": {"$exists": false}}'
```

The `fileAclAuthorizer` enforces what authenticated users can do based on their roles.

### Adding New Users and Roles

**To add a new user:**

1. Edit [users.yml](users.yml):
   ```yaml
   - userid: editor
     password: editor123
     roles:
       - editor
   ```

2. Add permissions for the role in [restheart.yml](restheart.yml):
   ```yaml
   - role: editor
     predicate: path-prefix[path=/shop/products]
     priority: 2
     mongo:
       readFilter: null
       writeFilter: null  # Can write to products
   ```

3. Restart the container: `docker-compose restart facet`

### Alternative Authenticators

RESTHeart supports multiple authentication methods. To use MongoDB-based users instead of files:

1. Enable `mongoRealmAuthenticator` in [restheart.yml](restheart.yml)
2. Disable `fileRealmAuthenticator`
3. Store users in MongoDB `restheart.users` collection

For production, consider:
- **MongoDB-based authentication** for dynamic user management
- **LDAP/Active Directory** for enterprise SSO
- **OAuth2/OIDC** for social login

See [RESTHeart Security Documentation](https://restheart.org/docs/security/overview) for complete configuration options.

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

## Troubleshooting

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
