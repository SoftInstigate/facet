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
- Facet core plugin built: `mvn -pl core package` from repository root

### Running the Example

1. **Build the Facet plugin** (from repository root):
   ```bash
   cd /path/to/facet
   mvn -pl core package -DskipTests
   ```

2. **Start the example** (from the example directory):
   ```bash
   cd examples/product-catalog
   docker-compose -f ../docker-compose.yml up --build
   ```

   The `--build` flag ensures the Facet Docker image is built with the plugin included.

3. **Wait for services to start** (watch logs for "RESTHeart started" message):
   ```bash
   docker-compose -f ../docker-compose.yml logs -f facet
   ```

4. **Access the application**:
   - Product Catalog: http://localhost:8080/shop/products
   - Facet API: http://localhost:8080/shop/products (with `Accept: application/json`)
   - Ping endpoint: http://localhost:8080/ping

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
│       ├── index.html              # Product list page
│       └── view.html               # Product detail page
└── _fragments/
    └── product-list.html           # Reusable product list (for HTMX)
```

### Configuration Files

- `restheart.yml` - Example-specific RESTHeart configuration
- `users.yml` - File-based user authentication and ACL rules
- `init-data.js` - MongoDB initialization script with sample products
- `static/` - Static assets (favicon, images, CSS, JS)

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
docker-compose -f ../docker-compose.yml logs -f facet

# MongoDB logs
docker-compose -f ../docker-compose.yml logs -f mongodb
```

### Testing the API

The same endpoints serve JSON for API clients:

```bash
# List products (JSON)
curl http://localhost:8080/shop/products

# Get single product
curl http://localhost:8080/shop/products/65abc123...

# Search with filter
curl "http://localhost:8080/shop/products?filter=%7B%22category%22%3A%22Electronics%22%7D"

# Create product (requires authentication)
curl -X POST http://localhost:8080/shop/products \
  -u admin:secret \
  -H "Content-Type: application/json" \
  -d '{"name":"New Product","price":99.99,"category":"Test","stock":10}'
```

### Stopping the Example

```bash
docker-compose -f ../docker-compose.yml down

# Remove data volume (reset to initial state)
docker-compose -f ../docker-compose.yml down -v
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
curl -X POST http://localhost:8080/shop/products \
  -u admin:secret \
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
- **Customize styling**: Replace Bulma with your preferred CSS framework
- **Add validation**: Form validation and error handling
- **Implement search**: Full-text search with MongoDB text indexes
- **Add images**: Product images with file upload

## Resources

- [Facet Developer's Guide](../../docs/DEVELOPERS_GUIDE.md)
- [Template Context Reference](../../docs/TEMPLATE_CONTEXT_REFERENCE.md)
- [Product Catalog Tutorial](../../docs/TUTORIAL_PRODUCT_CATALOG.md)
- [RESTHeart Documentation](https://restheart.org/docs/)
- [Pebble Templates](https://pebbletemplates.io/)
- [HTMX Documentation](https://htmx.org/)

## Troubleshooting

### Services won't start

```bash
# Check if ports are in use
lsof -i :8080
lsof -i :27017

# View detailed logs
docker-compose -f ../docker-compose.yml logs
```

### Template not found

- Check template path matches URL structure
- Ensure `templates/` directory is mounted in docker-compose.yml
- Check RESTHeart logs for template resolution attempts

### Authentication issues

- Verify users.yml syntax
- Check browser cookies (clear `rh_auth` cookie)
- Test with curl: `curl -u admin:secret http://localhost:8080/shop/products`

### Data not loading

- Check MongoDB logs: `docker-compose logs mongodb`
- Verify init-data.js syntax
- Manually insert data: `docker exec -it facet-examples-mongo mongosh`
