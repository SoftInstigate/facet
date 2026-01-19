# Facet Tutorial: Building a Product Catalog

This tutorial provides a progressive learning track for building a product catalog application with Facet. We'll start with the simplest possible example and iteratively add features until we have a complete, production-ready application.

> **💡 Working Example Available!**
> A complete, runnable product catalog example is available in the [`examples/product-catalog/`](../examples/product-catalog/) directory.
> Clone the repo, run `docker-compose up --build`, and see it in action at http://localhost:8080/shop/products
> See the [Product Catalog Example README](../examples/product-catalog/README.md) for details.

## Table of Contents

1. [Development Environment Setup](#1-development-environment-setup)
2. [Level 1: Hello World - Your First Template](#2-level-1-hello-world---your-first-template)
3. [Level 2: Display Product List](#3-level-2-display-product-list)
4. [Level 3: Product Details Page](#4-level-3-product-details-page)
5. [Level 4: Search and Filtering](#5-level-4-search-and-filtering)
6. [Level 5: Pagination](#6-level-5-pagination)
7. [Level 6: HTMX Partial Updates](#7-level-6-htmx-partial-updates)
8. [Level 7: Create/Edit/Delete](#8-level-7-createeditdelete)
9. [Level 8: Authentication and Authorization](#9-level-8-authentication-and-authorization)
10. [Level 9: Production Deployment](#10-level-9-production-deployment)

---

## 1. Development Environment Setup

### Using Docker Compose (Recommended)

Create a complete development environment with RESTHeart, MongoDB, and your Facet plugin in one command.

**Step 1:** Create a project directory structure:

```bash
mkdir facet-product-catalog
cd facet-product-catalog
mkdir -p templates/products plugins data
```

**Step 2:** Create `docker-compose.yml`:

```yaml
version: '3.8'

services:
  mongodb:
    image: mongo:8.0
    container_name: facet-mongo
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: secret
    ports:
      - "27017:27017"
    volumes:
      - ./data:/data/db
    networks:
      - facet-network

  restheart:
    image: softinstigate/restheart:latest
    container_name: facet-restheart
    depends_on:
      - mongodb
    ports:
      - "8080:8080"
    environment:
      MONGO_URI: mongodb://admin:secret@mongodb:27017/admin
      ROOT_ROLE: admin
      ROOT_PASSWORD: secret
    volumes:
      # Mount your Facet plugin JAR
      - ./plugins:/opt/restheart/plugins
      # Mount your templates directory
      - ./templates:/opt/restheart/templates
      # Mount custom configuration
      - ./restheart.yml:/opt/restheart/etc/restheart.yml
    networks:
      - facet-network
    command: ["java", "-jar", "/opt/restheart/restheart.jar", "/opt/restheart/etc/restheart.yml"]

networks:
  facet-network:
    driver: bridge
```

**Step 3:** Create minimal `restheart.yml` configuration:

```yaml
### RESTHeart Configuration for Facet Development

# HTTP Listener
http-listener:
  host: 0.0.0.0
  port: 8080

# MongoDB Connection
mongo-uri: mongodb://admin:secret@mongodb:27017/admin

# Plugins Directory
plugins-directory: /opt/restheart/plugins

# Enable Facet HTML Response Interceptor
/html-response-interceptor:
  enabled: true
  # Disable caching during development
  response-caching: false
  # Template base directory
  template-base-dir: /opt/restheart/templates

# MongoDB resource prefix (empty = mounted at root)
mongo-mounts:
  - what: /
    where: /

# CORS (for development - adjust for production)
cors:
  enabled: true
  allow-origin: "*"
  allow-methods: "*"
  allow-headers: "*"

# Authentication (basic setup - enhance for production)
auth:
  enabled: true
  mechanisms:
    - basic

# Security (adjust for your needs)
acl:
  - role: admin
    predicate: path-prefix[path=/]
    priority: 0
    mongo:
      read: true
      write: true
```

**Step 4:** Build and copy your Facet plugin:

```bash
# From your facet repository
cd /path/to/facet
mvn -pl core package -DskipTests

# Copy JAR to plugins directory
cp core/target/facet-core-*.jar /path/to/facet-product-catalog/plugins/
```

**Step 5:** Start the environment:

```bash
cd facet-product-catalog
docker-compose up -d
```

**Step 6:** Verify services are running:

```bash
# Check containers
docker-compose ps

# Test RESTHeart
curl -u admin:secret http://localhost:8080/

# Test MongoDB
docker exec -it facet-mongo mongosh -u admin -p secret --authenticationDatabase admin
```

**Step 7:** Load sample product data:

```bash
# Create products database and collection
curl -u admin:secret -X PUT http://localhost:8080/shop
curl -u admin:secret -X PUT http://localhost:8080/shop/products

# Insert sample products
curl -u admin:secret -X POST http://localhost:8080/shop/products \
  -H "Content-Type: application/json" \
  -d '[
    {"name": "Laptop", "price": 999.99, "category": "Electronics", "stock": 15},
    {"name": "Mouse", "price": 29.99, "category": "Electronics", "stock": 50},
    {"name": "Desk Chair", "price": 199.99, "category": "Furniture", "stock": 8},
    {"name": "Coffee Maker", "price": 79.99, "category": "Appliances", "stock": 20}
  ]'
```

### Manual Setup (Alternative)

If you prefer manual setup without Docker:

1. Install MongoDB locally
2. Install RESTHeart (download from https://restheart.org/docs/setup/)
3. Copy `facet-core.jar` to RESTHeart's `plugins/` directory
4. Configure `restheart.yml` as shown above
5. Create `templates/` directory in RESTHeart installation
6. Start services manually

---

## 2. Level 1: Hello World - Your First Template

**Learning Goal:** Understand path-based template resolution and see HTML instead of JSON.

### What You'll Build

A simple "Hello World" page at `/shop/products` that replaces the default JSON response.

### Step 1: Create the simplest template

Create `templates/shop/products/index.html`:

```html
<!DOCTYPE html>
<html>
<head>
    <title>Product Catalog</title>
</head>
<body>
    <h1>Hello from Facet!</h1>
    <p>This template was resolved for path: /shop/products</p>
</body>
</html>
```

### Step 2: Test it

Open your browser to: http://localhost:8080/shop/products

**Expected Result:** You see the HTML page instead of JSON.

**What just happened?**
- Browser requested `/shop/products` with `Accept: text/html` header
- Facet interceptor detected HTML request
- Template resolver found `templates/shop/products/index.html`
- Template was rendered and returned as HTML

### Step 3: Verify API still works

```bash
curl -u admin:secret http://localhost:8080/shop/products
```

**Expected Result:** You get JSON! The API is unchanged for non-browser clients.

**Key Concept:** Templates are **opt-in**. No template = JSON API. With template = HTML UI. Same endpoint, dual interface.

---

## 3. Level 2: Display Product List

**Learning Goal:** Use template context variables to display actual MongoDB data.

### What You'll Build

A template that shows all products from the database in a simple HTML list.

### Step 1: Update the template with context variables

Update `templates/shop/products/index.html`:

```html
<!DOCTYPE html>
<html>
<head>
    <title>Product Catalog</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        .product {
            border: 1px solid #ddd;
            padding: 15px;
            margin: 10px 0;
            border-radius: 5px;
        }
        .price { color: green; font-weight: bold; }
        .stock { color: #666; font-size: 0.9em; }
    </style>
</head>
<body>
    <h1>Product Catalog</h1>

    {% if items is not empty %}
        <p>Found {{ items | length }} products</p>

        {% for doc in items %}
            <div class="product">
                <h3>{{ doc.data.name }}</h3>
                <p class="price">${{ doc.data.price }}</p>
                <p>Category: {{ doc.data.category }}</p>
                <p class="stock">In stock: {{ doc.data.stock }}</p>
            </div>
        {% endfor %}
    {% else %}
        <p>No products found.</p>
    {% endif %}
</body>
</html>
```

### Step 2: Test it

Reload: http://localhost:8080/shop/products

**Expected Result:** You see all your products displayed in styled boxes.

### Key Concepts

**Context Variable: `items`**
- Contains list of documents from MongoDB collection
- Each item has `doc.data` (BSON document) and metadata (like `doc._id`)

**Pebble Template Syntax:**
- `{% if items is not empty %}` - conditional logic
- `{% for doc in items %}` - loop over collection
- `{{ doc.data.name }}` - output variable value

### Step 3: Explore other context variables

Add this debug section to your template (before `</body>`):

```html
<hr>
<h3>Available Context Variables</h3>
<ul>
    <li><strong>path:</strong> {{ path }}</li>
    <li><strong>mongoPath:</strong> {{ mongoPath }}</li>
    <li><strong>database:</strong> {{ database }}</li>
    <li><strong>collection:</strong> {{ collection }}</li>
    <li><strong>resourceType:</strong> {{ resourceType }}</li>
    <li><strong>username:</strong> {{ username | default('Not authenticated') }}</li>
</ul>
```

Reload and see all context variables in action.

---

## 4. Level 3: Product Details Page

**Learning Goal:** Create document-specific templates with hierarchical resolution.

### What You'll Build

A detail page for individual products at `/shop/products/{id}`.

### Step 1: Create a view template

Create `templates/shop/products/view.html`:

```html
<!DOCTYPE html>
<html>
<head>
    <title>Product Details</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        .product-detail {
            max-width: 600px;
            border: 2px solid #333;
            padding: 30px;
            border-radius: 10px;
        }
        .price { font-size: 2em; color: green; margin: 20px 0; }
        .back-link {
            display: inline-block;
            margin-top: 20px;
            color: #0066cc;
            text-decoration: none;
        }
        .back-link:hover { text-decoration: underline; }
    </style>
</head>
<body>
    <div class="product-detail">
        {% if items is not empty %}
            {% set product = items[0] %}

            <h1>{{ product.data.name }}</h1>

            <div class="price">${{ product.data.price }}</div>

            <p><strong>Category:</strong> {{ product.data.category }}</p>
            <p><strong>In Stock:</strong> {{ product.data.stock }} units</p>

            <p><strong>Product ID:</strong> <code>{{ product._id.value }}</code></p>

            <a href="{{ path | parentPath }}" class="back-link">← Back to Product List</a>
        {% else %}
            <p>Product not found.</p>
            <a href="{{ path | parentPath }}" class="back-link">← Back to Product List</a>
        {% endif %}
    </div>
</body>
</html>
```

### Step 2: Add links to list page

Update `templates/shop/products/index.html` to add links:

```html
{% for doc in items %}
    <div class="product">
        <h3>
            <a href="{{ path }}/{{ doc._id.value }}">{{ doc.data.name }}</a>
        </h3>
        <p class="price">${{ doc.data.price }}</p>
        <p>Category: {{ doc.data.category }}</p>
        <p class="stock">In stock: {{ doc.data.stock }}</p>
    </div>
{% endfor %}
```

### Step 3: Test it

1. Go to http://localhost:8080/shop/products
2. Click on a product name
3. See the detailed view
4. Click "Back to Product List"

### Template Resolution in Action

When you request `/shop/products/65abc123...`:

**Facet looks for templates in this order:**
1. `templates/shop/products/65abc123.../view.html` (document-specific, doesn't exist)
2. `templates/shop/products/view.html` ✓ **FOUND**
3. `templates/shop/products/index.html` (fallback)
4. `templates/shop/index.html` (parent fallback)
5. `templates/index.html` (root fallback)

This is **hierarchical resolution** - Facet walks up the path until it finds a template.

---

## 5. Level 4: Search and Filtering

**Learning Goal:** Use MongoDB query parameters to filter and search products.

### What You'll Build

A search form that filters products by category and price range.

### Step 1: Add search form to list page

Update `templates/shop/products/index.html`, add this after the `<h1>`:

```html
<form method="GET" action="{{ path }}">
    <div style="background: #f5f5f5; padding: 20px; border-radius: 5px; margin-bottom: 20px;">
        <h3>Search Products</h3>

        <label>
            Category:
            <select name="category">
                <option value="">All Categories</option>
                <option value="Electronics">Electronics</option>
                <option value="Furniture">Furniture</option>
                <option value="Appliances">Appliances</option>
            </select>
        </label>

        <label style="margin-left: 20px;">
            Max Price:
            <input type="number" name="maxPrice" placeholder="999.99" step="0.01">
        </label>

        <button type="submit" style="margin-left: 20px; padding: 8px 16px;">Search</button>
        <a href="{{ path }}" style="margin-left: 10px;">Clear</a>
    </div>
</form>
```

### Step 2: Use MongoDB filter parameter

Update the form to use MongoDB query syntax:

```html
<form method="GET" action="{{ path }}" id="searchForm">
    <div style="background: #f5f5f5; padding: 20px; border-radius: 5px; margin-bottom: 20px;">
        <h3>Search Products</h3>

        <label>
            Category:
            <select id="categorySelect">
                <option value="">All Categories</option>
                <option value="Electronics">Electronics</option>
                <option value="Furniture">Furniture</option>
                <option value="Appliances">Appliances</option>
            </select>
        </label>

        <label style="margin-left: 20px;">
            Max Price:
            <input type="number" id="maxPriceInput" placeholder="999.99" step="0.01">
        </label>

        <!-- Hidden field for MongoDB filter -->
        <input type="hidden" name="filter" id="filterInput">

        <button type="submit" style="margin-left: 20px; padding: 8px 16px;">Search</button>
        <a href="{{ path }}" style="margin-left: 10px;">Clear</a>
    </div>
</form>

<script>
document.getElementById('searchForm').addEventListener('submit', function(e) {
    e.preventDefault();

    const category = document.getElementById('categorySelect').value;
    const maxPrice = document.getElementById('maxPriceInput').value;

    // Build MongoDB filter
    let filter = {};
    if (category) {
        filter.category = category;
    }
    if (maxPrice) {
        filter.price = { $lte: parseFloat(maxPrice) };
    }

    // Set hidden field and submit
    document.getElementById('filterInput').value = JSON.stringify(filter);
    this.submit();
});
</script>
```

### Step 3: Test filtering

1. Go to http://localhost:8080/shop/products
2. Select "Electronics" category → See only electronics
3. Set max price to 50 → See only items under $50
4. Check URL: `?filter={"category":"Electronics","price":{"$lte":50}}`

### MongoDB Query Parameters

RESTHeart supports powerful query parameters:

- **`filter`** - MongoDB query in JSON: `?filter={"category":"Electronics"}`
- **`sort`** - Sort specification: `?sort={"price":1}` (ascending) or `{"price":-1}` (descending)
- **`keys`** - Field projection: `?keys={"name":1,"price":1}` (only include these fields)
- **`page`** - Page number: `?page=1`
- **`pagesize`** - Items per page: `?pagesize=10`

These are available as context variables: `{{ filter }}`, `{{ sort }}`, `{{ keys }}`, `{{ page }}`, `{{ pagesize }}`

---

## 6. Level 5: Pagination

**Learning Goal:** Handle large datasets with pagination controls.

### What You'll Build

Previous/Next page navigation for product lists.

### Step 1: Add pagination info display

Add this after the search form in `templates/shop/products/index.html`:

```html
<div style="margin-bottom: 20px;">
    {% if totalItems > 0 %}
        <p>
            Showing {{ (page - 1) * pagesize + 1 }} to
            {{ (page - 1) * pagesize + (items | length) }}
            of {{ totalItems }} products
        </p>
    {% endif %}
</div>
```

### Step 2: Add pagination controls

Add this before `</body>`:

```html
{% if totalPages > 1 %}
<div style="margin-top: 30px; padding: 20px; background: #f5f5f5; border-radius: 5px;">
    <strong>Pages:</strong>

    {% if page > 1 %}
        <a href="?page={{ page - 1 }}&pagesize={{ pagesize }}{% if filter %}&filter={{ filter | urlencode }}{% endif %}">
            ← Previous
        </a>
    {% endif %}

    <span style="margin: 0 15px;">Page {{ page }} of {{ totalPages }}</span>

    {% if page < totalPages %}
        <a href="?page={{ page + 1 }}&pagesize={{ pagesize }}{% if filter %}&filter={{ filter | urlencode }}{% endif %}">
            Next →
        </a>
    {% endif %}
</div>
{% endif %}
```

### Step 3: Test pagination

```bash
# Load more products to test pagination
for i in {1..50}; do
  curl -u admin:secret -X POST http://localhost:8080/shop/products \
    -H "Content-Type: application/json" \
    -d "{\"name\": \"Product $i\", \"price\": $((RANDOM % 500 + 10)), \"category\": \"Test\", \"stock\": $((RANDOM % 100))}"
done
```

Now visit: http://localhost:8080/shop/products?pagesize=10

You should see pagination controls appear.

### Pagination Context Variables

- **`page`** - Current page number (1-indexed)
- **`pagesize`** - Items per page
- **`totalItems`** - Total document count
- **`totalPages`** - Calculated: `ceil(totalItems / pagesize)`

---

## 7. Level 6: HTMX Partial Updates

**Learning Goal:** Use HTMX for dynamic updates without full page reloads.

### What You'll Build

A product list that updates via HTMX when filtering, without reloading the entire page.

### Step 1: Add HTMX library

Update the `<head>` section in `templates/shop/products/index.html`:

```html
<head>
    <title>Product Catalog</title>
    <script src="https://unpkg.com/htmx.org@1.9.10"></script>
    <style>
        /* ... existing styles ... */

        /* HTMX loading indicator */
        .htmx-indicator {
            display: none;
            color: #666;
            font-style: italic;
        }
        .htmx-request .htmx-indicator {
            display: inline;
        }
    </style>
</head>
```

### Step 2: Create a fragment template

Create `templates/_fragments/product-list.html`:

```html
{% parallel %}
<div id="product-list">
    {% if items is not empty %}
        <p>Found {{ items | length }} products</p>

        {% for doc in items %}
            <div class="product">
                <h3>
                    <a href="{{ path }}/{{ doc._id.value }}">{{ doc.data.name }}</a>
                </h3>
                <p class="price">${{ doc.data.price }}</p>
                <p>Category: {{ doc.data.category }}</p>
                <p class="stock">In stock: {{ doc.data.stock }}</p>
            </div>
        {% endfor %}
    {% else %}
        <p>No products found.</p>
    {% endif %}

    {% include "_fragments/pagination" %}
</div>
{% endparallel %}
```

### Step 3: Create pagination fragment

Create `templates/_fragments/pagination.html`:

```html
{% parallel %}
{% if totalPages > 1 %}
<div style="margin-top: 30px; padding: 20px; background: #f5f5f5; border-radius: 5px;">
    <strong>Pages:</strong>

    {% if page > 1 %}
        <a href="?page={{ page - 1 }}&pagesize={{ pagesize }}{% if filter %}&filter={{ filter | urlencode }}{% endif %}"
           hx-get="{{ path }}?page={{ page - 1 }}&pagesize={{ pagesize }}{% if filter %}&filter={{ filter | urlencode }}{% endif %}"
           hx-target="#product-list"
           hx-swap="outerHTML">
            ← Previous
        </a>
    {% endif %}

    <span style="margin: 0 15px;">Page {{ page }} of {{ totalPages }}</span>

    {% if page < totalPages %}
        <a href="?page={{ page + 1 }}&pagesize={{ pagesize }}{% if filter %}&filter={{ filter | urlencode }}{% endif %}"
           hx-get="{{ path }}?page={{ page + 1 }}&pagesize={{ pagesize }}{% if filter %}&filter={{ filter | urlencode }}{% endif %}"
           hx-target="#product-list"
           hx-swap="outerHTML">
            Next →
        </a>
    {% endif %}
</div>
{% endif %}
{% endparallel %}
```

### Step 4: Update main template to use fragments

Update `templates/shop/products/index.html`:

```html
<body>
    <h1>Product Catalog</h1>

    <form method="GET" action="{{ path }}" id="searchForm"
          hx-get="{{ path }}"
          hx-target="#product-list"
          hx-swap="outerHTML"
          hx-indicator="#loading">

        <div style="background: #f5f5f5; padding: 20px; border-radius: 5px; margin-bottom: 20px;">
            <h3>Search Products <span id="loading" class="htmx-indicator">Loading...</span></h3>

            <!-- ... search form fields ... -->

            <button type="submit" style="margin-left: 20px; padding: 8px 16px;">Search</button>
            <a href="{{ path }}" style="margin-left: 10px;">Clear</a>
        </div>
    </form>

    {% include "_fragments/product-list" %}
</body>
```

### Step 5: Test HTMX updates

1. Go to http://localhost:8080/shop/products
2. Open browser DevTools Network tab
3. Change search filters and click Search
4. Notice: Only the product list updates, not the entire page!

### How HTMX Fragment Resolution Works

When HTMX makes a request with `hx-target="#product-list"`:

1. HTMX sends header: `HX-Request: true` and `HX-Target: product-list`
2. Facet detects HTMX request via `HtmxRequestDetector`
3. Template resolver looks for: `templates/shop/products/_fragments/product-list.html`
4. If not found, falls back to: `templates/_fragments/product-list.html`
5. **Strict mode**: If still not found → 500 error (fragments must exist)

---

## 8. Level 7: Create/Edit/Delete

**Learning Goal:** Full CRUD operations with forms and RESTHeart API integration.

### What You'll Build

Forms to create new products, edit existing ones, and delete them.

### Step 1: Add "Create Product" button

Add to `templates/shop/products/index.html` after the `<h1>`:

```html
<div style="margin-bottom: 20px;">
    <a href="{{ path }}/new" style="background: #0066cc; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">
        + Create New Product
    </a>
</div>
```

### Step 2: Create product form template

Create `templates/shop/products/new/index.html`:

```html
<!DOCTYPE html>
<html>
<head>
    <title>Create Product</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        .form-container { max-width: 600px; }
        .form-group { margin-bottom: 15px; }
        .form-group label { display: block; margin-bottom: 5px; font-weight: bold; }
        .form-group input, .form-group select {
            width: 100%;
            padding: 8px;
            border: 1px solid #ddd;
            border-radius: 4px;
        }
        .button {
            padding: 10px 20px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            text-decoration: none;
            display: inline-block;
        }
        .button-primary { background: #0066cc; color: white; }
        .button-secondary { background: #ddd; color: #333; }
    </style>
</head>
<body>
    <div class="form-container">
        <h1>Create New Product</h1>

        <form id="productForm">
            <div class="form-group">
                <label for="name">Product Name</label>
                <input type="text" id="name" required>
            </div>

            <div class="form-group">
                <label for="price">Price</label>
                <input type="number" id="price" step="0.01" required>
            </div>

            <div class="form-group">
                <label for="category">Category</label>
                <select id="category" required>
                    <option value="">Select category...</option>
                    <option value="Electronics">Electronics</option>
                    <option value="Furniture">Furniture</option>
                    <option value="Appliances">Appliances</option>
                </select>
            </div>

            <div class="form-group">
                <label for="stock">Stock</label>
                <input type="number" id="stock" required>
            </div>

            <div style="margin-top: 20px;">
                <button type="submit" class="button button-primary">Create Product</button>
                <a href="{{ path | parentPath }}" class="button button-secondary">Cancel</a>
            </div>
        </form>

        <div id="message" style="margin-top: 20px; padding: 10px; border-radius: 4px; display: none;"></div>
    </div>

    <script>
    document.getElementById('productForm').addEventListener('submit', async function(e) {
        e.preventDefault();

        const product = {
            name: document.getElementById('name').value,
            price: parseFloat(document.getElementById('price').value),
            category: document.getElementById('category').value,
            stock: parseInt(document.getElementById('stock').value)
        };

        try {
            const response = await fetch('{{ path | parentPath }}', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Basic ' + btoa('admin:secret')
                },
                body: JSON.stringify(product)
            });

            if (response.ok) {
                // Redirect to product list
                window.location.href = '{{ path | parentPath }}';
            } else {
                showMessage('Error creating product: ' + response.statusText, 'error');
            }
        } catch (error) {
            showMessage('Error: ' + error.message, 'error');
        }
    });

    function showMessage(text, type) {
        const msg = document.getElementById('message');
        msg.textContent = text;
        msg.style.display = 'block';
        msg.style.background = type === 'error' ? '#ffdddd' : '#ddffdd';
        msg.style.color = type === 'error' ? '#cc0000' : '#006600';
    }
    </script>
</body>
</html>
```

### Step 3: Add edit/delete buttons to product list

Update the product display loop in `templates/_fragments/product-list.html`:

```html
{% for doc in items %}
    <div class="product">
        <h3>
            <a href="{{ path }}/{{ doc._id.value }}">{{ doc.data.name }}</a>
        </h3>
        <p class="price">${{ doc.data.price }}</p>
        <p>Category: {{ doc.data.category }}</p>
        <p class="stock">In stock: {{ doc.data.stock }}</p>

        <div style="margin-top: 10px;">
            <a href="{{ path }}/{{ doc._id.value }}/edit"
               style="padding: 5px 10px; background: #0066cc; color: white; text-decoration: none; border-radius: 3px; font-size: 0.9em;">
                Edit
            </a>
            <button onclick="deleteProduct('{{ doc._id.value }}')"
                    style="padding: 5px 10px; background: #cc0000; color: white; border: none; border-radius: 3px; cursor: pointer; font-size: 0.9em;">
                Delete
            </button>
        </div>
    </div>
{% endfor %}

<script>
async function deleteProduct(id) {
    if (!confirm('Are you sure you want to delete this product?')) {
        return;
    }

    try {
        const response = await fetch('{{ path }}/' + id, {
            method: 'DELETE',
            headers: {
                'Authorization': 'Basic ' + btoa('admin:secret')
            }
        });

        if (response.ok) {
            // Reload page to show updated list
            window.location.reload();
        } else {
            alert('Error deleting product: ' + response.statusText);
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}
</script>
```

### Step 4: Create edit template

Create `templates/shop/products/edit/view.html`:

```html
<!DOCTYPE html>
<html>
<head>
    <title>Edit Product</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        .form-container { max-width: 600px; }
        .form-group { margin-bottom: 15px; }
        .form-group label { display: block; margin-bottom: 5px; font-weight: bold; }
        .form-group input, .form-group select {
            width: 100%;
            padding: 8px;
            border: 1px solid #ddd;
            border-radius: 4px;
        }
        .button {
            padding: 10px 20px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            text-decoration: none;
            display: inline-block;
        }
        .button-primary { background: #0066cc; color: white; }
        .button-secondary { background: #ddd; color: #333; }
    </style>
</head>
<body>
    <div class="form-container">
        <h1>Edit Product</h1>

        {% if items is not empty %}
            {% set product = items[0] %}

            <form id="productForm">
                <div class="form-group">
                    <label for="name">Product Name</label>
                    <input type="text" id="name" value="{{ product.data.name }}" required>
                </div>

                <div class="form-group">
                    <label for="price">Price</label>
                    <input type="number" id="price" step="0.01" value="{{ product.data.price }}" required>
                </div>

                <div class="form-group">
                    <label for="category">Category</label>
                    <select id="category" required>
                        <option value="Electronics" {% if product.data.category == "Electronics" %}selected{% endif %}>Electronics</option>
                        <option value="Furniture" {% if product.data.category == "Furniture" %}selected{% endif %}>Furniture</option>
                        <option value="Appliances" {% if product.data.category == "Appliances" %}selected{% endif %}>Appliances</option>
                    </select>
                </div>

                <div class="form-group">
                    <label for="stock">Stock</label>
                    <input type="number" id="stock" value="{{ product.data.stock }}" required>
                </div>

                <div style="margin-top: 20px;">
                    <button type="submit" class="button button-primary">Save Changes</button>
                    <a href="{{ path | parentPath | parentPath }}" class="button button-secondary">Cancel</a>
                </div>
            </form>

            <script>
            document.getElementById('productForm').addEventListener('submit', async function(e) {
                e.preventDefault();

                const updated = {
                    name: document.getElementById('name').value,
                    price: parseFloat(document.getElementById('price').value),
                    category: document.getElementById('category').value,
                    stock: parseInt(document.getElementById('stock').value)
                };

                try {
                    const response = await fetch('{{ path | parentPath }}', {
                        method: 'PATCH',
                        headers: {
                            'Content-Type': 'application/json',
                            'Authorization': 'Basic ' + btoa('admin:secret')
                        },
                        body: JSON.stringify(updated)
                    });

                    if (response.ok) {
                        window.location.href = '{{ path | parentPath | parentPath }}';
                    } else {
                        alert('Error updating product: ' + response.statusText);
                    }
                } catch (error) {
                    alert('Error: ' + error.message);
                }
            });
            </script>
        {% else %}
            <p>Product not found.</p>
        {% endif %}
    </div>
</body>
</html>
```

### Step 5: Test CRUD operations

1. **Create**: Click "+ Create New Product", fill form, submit
2. **Read**: See new product in list
3. **Update**: Click "Edit" on a product, change values, save
4. **Delete**: Click "Delete" on a product, confirm deletion

---

## 9. Level 8: Authentication and Authorization

**Learning Goal:** Protect resources with authentication and role-based access control.

### What You'll Build

Login page, protected CRUD operations, and role-based visibility.

### Step 1: Configure authentication in restheart.yml

Update your `restheart.yml`:

```yaml
auth:
  enabled: true
  mechanisms:
    - basic
    - token

# Define users
users:
  - userid: admin
    password: secret
    roles: [admin]
  - userid: viewer
    password: viewer
    roles: [viewer]

# Define ACL rules
acl:
  # Admin can do anything
  - role: admin
    predicate: path-prefix[path=/]
    priority: 0
    mongo:
      read: true
      write: true

  # Viewer can only read
  - role: viewer
    predicate: path-prefix[path=/shop]
    priority: 1
    mongo:
      read: true
      write: false
```

### Step 2: Create login template

Create `templates/login/index.html`:

```html
<!DOCTYPE html>
<html>
<head>
    <title>Login</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100vh;
            margin: 0;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        }
        .login-container {
            background: white;
            padding: 40px;
            border-radius: 10px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
            width: 300px;
        }
        .form-group {
            margin-bottom: 20px;
        }
        .form-group label {
            display: block;
            margin-bottom: 5px;
            font-weight: bold;
            color: #333;
        }
        .form-group input {
            width: 100%;
            padding: 10px;
            border: 1px solid #ddd;
            border-radius: 4px;
            box-sizing: border-box;
        }
        .button {
            width: 100%;
            padding: 12px;
            background: #667eea;
            color: white;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 16px;
            font-weight: bold;
        }
        .button:hover {
            background: #5568d3;
        }
        .error {
            color: #cc0000;
            margin-top: 10px;
            display: none;
        }
    </style>
</head>
<body>
    <div class="login-container">
        <h2 style="margin-top: 0; text-align: center;">Product Catalog Login</h2>

        <form id="loginForm">
            <div class="form-group">
                <label for="username">Username</label>
                <input type="text" id="username" required>
            </div>

            <div class="form-group">
                <label for="password">Password</label>
                <input type="password" id="password" required>
            </div>

            <button type="submit" class="button">Login</button>

            <div class="error" id="error">Invalid credentials</div>
        </form>

        <div style="margin-top: 20px; font-size: 0.9em; color: #666;">
            <p>Demo accounts:</p>
            <ul>
                <li><strong>admin</strong> / secret (full access)</li>
                <li><strong>viewer</strong> / viewer (read-only)</li>
            </ul>
        </div>
    </div>

    <script>
    document.getElementById('loginForm').addEventListener('submit', async function(e) {
        e.preventDefault();

        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;

        try {
            const response = await fetch('/shop/products', {
                headers: {
                    'Authorization': 'Basic ' + btoa(username + ':' + password)
                }
            });

            if (response.ok) {
                // Store credentials (in real app, use tokens)
                sessionStorage.setItem('auth', btoa(username + ':' + password));
                window.location.href = '/shop/products';
            } else {
                document.getElementById('error').style.display = 'block';
            }
        } catch (error) {
            document.getElementById('error').style.display = 'block';
        }
    });
    </script>
</body>
</html>
```

### Step 3: Add role-based UI visibility

Update `templates/shop/products/index.html` to show/hide buttons based on roles:

```html
<div style="margin-bottom: 20px;">
    {% if roles and 'admin' in roles %}
        <a href="{{ path }}/new" style="background: #0066cc; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">
            + Create New Product
        </a>
    {% endif %}
</div>
```

Update product list to show/hide edit/delete:

```html
{% if roles and 'admin' in roles %}
<div style="margin-top: 10px;">
    <a href="{{ path }}/{{ doc._id.value }}/edit"
       style="padding: 5px 10px; background: #0066cc; color: white; text-decoration: none; border-radius: 3px; font-size: 0.9em;">
        Edit
    </a>
    <button onclick="deleteProduct('{{ doc._id.value }}')"
            style="padding: 5px 10px; background: #cc0000; color: white; border: none; border-radius: 3px; cursor: pointer; font-size: 0.9em;">
        Delete
    </button>
</div>
{% endif %}
```

### Step 4: Add user info display

Add to top of page in `templates/shop/products/index.html`:

```html
<div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
    <h1 style="margin: 0;">Product Catalog</h1>

    {% if username %}
        <div>
            <span>Logged in as: <strong>{{ username }}</strong></span>
            <span style="margin-left: 10px;">Role: <strong>{{ roles | join(', ') }}</strong></span>
        </div>
    {% endif %}
</div>
```

### Authentication Context Variables

- **`username`** - Current authenticated user (null if not authenticated)
- **`roles`** - List of user roles (e.g., `['admin']`, `['viewer']`)

---

## 10. Level 9: Production Deployment

**Learning Goal:** Deploy to production with security, caching, and monitoring.

### Production Checklist

#### 1. Security Configuration

Update `restheart.yml` for production:

```yaml
# Use environment variables for secrets
mongo-uri: ${MONGO_URI}

auth:
  enabled: true
  mechanisms:
    - token  # Use token auth, not basic

  # Configure JWT tokens
  jwt:
    secret: ${JWT_SECRET}
    issuer: facet-catalog
    audience: facet-catalog
    ttl: 3600  # 1 hour

# Strict CORS
cors:
  enabled: true
  allow-origin: "https://yourdomain.com"
  allow-credentials: true

# Enable HTTPS
https-listener:
  enabled: true
  host: 0.0.0.0
  port: 8443
  keystore-file: /path/to/keystore.jks
  keystore-password: ${KEYSTORE_PASSWORD}

# Enable response caching
/html-response-interceptor:
  enabled: true
  response-caching: true
  max-age: 300  # 5 minutes
```

#### 2. Enable Template Caching

In production, Facet caches rendered templates:

- ETag headers generated from template hash
- 304 Not Modified responses for cached content
- Configurable `max-age` for cache duration

#### 3. MongoDB Optimization

```yaml
# Connection pooling
mongo-options:
  max-pool-size: 50
  min-pool-size: 5
  max-idle-time-ms: 60000
  max-connection-life-time-ms: 600000

# Read preference
mongo-read-preference: secondaryPreferred
```

#### 4. Monitoring and Logging

```yaml
# Enable request logging
request-log:
  enabled: true
  level: INFO
  format: "{{method}} {{uri}} {{status}} {{elapsed-time}}ms"

# Health check endpoint
/ping:
  enabled: true
```

#### 5. Docker Compose for Production

Create `docker-compose.prod.yml`:

```yaml
version: '3.8'

services:
  mongodb:
    image: mongo:7.0
    restart: always
    environment:
      MONGO_INITDB_ROOT_USERNAME: ${MONGO_USER}
      MONGO_INITDB_ROOT_PASSWORD: ${MONGO_PASSWORD}
    volumes:
      - mongo-data:/data/db
    networks:
      - backend

  restheart:
    image: softinstigate/restheart:latest
    restart: always
    depends_on:
      - mongodb
    ports:
      - "8443:8443"
    environment:
      MONGO_URI: mongodb://${MONGO_USER}:${MONGO_PASSWORD}@mongodb:27017/admin
      JWT_SECRET: ${JWT_SECRET}
      KEYSTORE_PASSWORD: ${KEYSTORE_PASSWORD}
    volumes:
      - ./plugins:/opt/restheart/plugins
      - ./templates:/opt/restheart/templates
      - ./restheart.prod.yml:/opt/restheart/etc/restheart.yml
      - ./certs:/opt/restheart/certs
    networks:
      - backend
      - frontend

  nginx:
    image: nginx:alpine
    restart: always
    depends_on:
      - restheart
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./certs:/etc/nginx/certs
    networks:
      - frontend

volumes:
  mongo-data:

networks:
  backend:
    internal: true
  frontend:
```

#### 6. Environment Variables

Create `.env.prod`:

```bash
MONGO_USER=produser
MONGO_PASSWORD=<strong-password>
JWT_SECRET=<long-random-string>
KEYSTORE_PASSWORD=<keystore-password>
```

**Never commit `.env.prod` to version control!**

#### 7. Deployment Commands

```bash
# Build plugin
mvn -pl core package -DskipTests

# Copy to production server
scp core/target/facet-core-*.jar user@server:/opt/facet/plugins/

# Deploy with Docker Compose
ssh user@server "cd /opt/facet && docker-compose -f docker-compose.prod.yml up -d"

# Check logs
ssh user@server "docker-compose -f /opt/facet/docker-compose.prod.yml logs -f restheart"
```

---

## Summary: What You've Learned

1. **Path-Based Templates** - Drop templates where paths match resources
2. **Template Context** - Rich variables from MongoDB queries
3. **Hierarchical Resolution** - Template fallback up the path tree
4. **MongoDB Queries** - `filter`, `sort`, `keys` parameters
5. **Pagination** - Handle large datasets with page controls
6. **HTMX Fragments** - Partial updates without full reloads
7. **CRUD Operations** - Create, edit, delete with RESTHeart API
8. **Authentication** - Secure resources with roles
9. **Production Ready** - Caching, security, monitoring

## Next Steps

- **Custom Extensions**: Add custom Pebble filters and functions
- **Advanced HTMX**: Use triggers, events, and server-sent events
- **Real-time Updates**: WebSocket integration
- **Complex Queries**: MongoDB aggregation pipelines
- **Multi-Tenancy**: Database-per-tenant patterns
- **Testing**: Integration tests with Testcontainers

## Resources

- [Facet Developer's Guide](DEVELOPERS_GUIDE.md)
- [Template Context Reference](TEMPLATE_CONTEXT_REFERENCE.md)
- [RESTHeart Documentation](https://restheart.org/docs/)
- [Pebble Templates](https://pebbletemplates.io/)
- [HTMX Documentation](https://htmx.org/docs/)

---

**Happy building with Facet!** 🚀
