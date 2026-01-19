# Facet Examples

This directory contains runnable examples demonstrating Facet's features. Each example is self-contained with templates, configuration, and sample data.

## Quick Start

### Prerequisites

- Docker and Docker Compose
- Java 25+ and Maven (to build Facet)

### Running Any Example

1. **Build Facet core plugin** (from repository root):
   ```bash
   cd /path/to/facet
   mvn -pl core package -DskipTests
   ```

2. **Navigate to an example**:
   ```bash
   cd examples/product-catalog
   ```

3. **Start with Docker Compose**:
   ```bash
   docker-compose -f ../docker-compose.yml up --build
   ```

   The `--build` flag builds the Facet Docker image with the plugin included. On first run, this may take a minute.

4. **Access the application** (check example README for URL)

## Available Examples

### 1. Product Catalog

**Directory**: [`product-catalog/`](product-catalog/)

A complete e-commerce product catalog demonstrating:
- MongoDB data binding and display
- Search and filtering with MongoDB queries
- HTMX partial updates for search/pagination
- Authentication and role-based access control
- Full CRUD operations (admin only)
- Template inheritance and fragments

**Start here** if you're new to Facet!

[→ Product Catalog README](product-catalog/README.md)

---

## Example Structure

Each example follows this structure:

```
example-name/
├── README.md              # Example-specific documentation
├── restheart.yml          # RESTHeart configuration (extends base)
├── users.yml              # Authentication users (if needed)
├── init-data.js           # MongoDB initialization script
├── static/                # Static assets (favicon, images, CSS, JS)
│   ├── favicon.ico        # Prevents browser 401 auth popup
│   └── ...
└── templates/             # Facet templates
    ├── layout.html        # Base layout (optional)
    ├── resource/          # Resource-specific templates
    │   └── index.html
    └── _fragments/        # HTMX fragments
        └── component.html
```

## Shared Infrastructure

All examples share common infrastructure in the `examples/` directory:

### Docker Compose (`docker-compose.yml`)

Provides MongoDB and Facet containers with:
- MongoDB 7.0 with data persistence
- Facet (built from local source with plugin included)
- Automatic mounting of templates and configurations
- Healthchecks for service readiness

### Dockerfile (`Dockerfile`)

Builds a custom Facet image based on `softinstigate/restheart:latest` with:
- **Facet core plugin** pre-installed at `/opt/restheart/plugins/facet-core.jar`
- **Default configuration** baked in at `/opt/restheart/etc/restheart.yml` (from `shared/restheart-base.yml`)
- Development-friendly settings: hot-reload enabled, caching disabled, file-based template loading

The image is fully functional and can run standalone with just a MongoDB connection:
```bash
docker run -e MONGO_URI=mongodb://host:27017/admin -p 8080:8080 facet:latest
```

### Base Configuration (`shared/restheart-base.yml`)

Default configuration baked into the Facet image:
- MongoDB connection (via `MONGO_URI` environment variable)
- Facet plugin enabled with hot-reload and no caching
- Authentication mechanisms (JWT, cookies)
- Development-friendly logging (debug level, full stack traces)
- All Facet interceptors enabled
- **ACL rule**: Allows unauthenticated access to common static assets (`/favicon.ico`, `/apple-touch-icon*`, `/robots.txt`, `/static/*`) to prevent browser auth popups

Examples can override this by mounting their own `restheart.yml` file.

## Development Workflow

### Hot Reload Templates

Templates are loaded from the filesystem with caching disabled for development:

1. Edit any template file
2. Refresh your browser
3. See changes immediately - no restart needed!

### Viewing Logs

```bash
# From any example directory

# Facet/RESTHeart logs
docker-compose -f ../docker-compose.yml logs -f facet

# MongoDB logs
docker-compose -f ../docker-compose.yml logs -f mongodb

# All services
docker-compose -f ../docker-compose.yml logs -f
```

### Stopping Services

```bash
# Stop and remove containers
docker-compose -f ../docker-compose.yml down

# Also remove data volumes (reset to initial state)
docker-compose -f ../docker-compose.yml down -v
```

### Accessing MongoDB

```bash
# Connect to MongoDB shell
docker exec -it facet-examples-mongo mongosh -u admin -p secret --authenticationDatabase admin

# Run queries
> use shop
> db.products.find()
> db.products.countDocuments()
```

### Testing API Endpoints

All examples serve both HTML (for browsers) and JSON (for API clients):

```bash
# HTML response (in browser or with Accept header)
curl http://localhost:8080/shop/products

# JSON response (explicit header)
curl -H "Accept: application/json" http://localhost:8080/shop/products

# With authentication
curl -u admin:secret http://localhost:8080/shop/products
```

## Creating New Examples

To create a new example:

1. **Create example directory**:
   ```bash
   cd examples
   mkdir my-example
   cd my-example
   ```

2. **Create minimal structure**:
   ```
   my-example/
   ├── README.md
   ├── restheart.yml
   ├── init-data.js
   └── templates/
       └── myresource/
           └── index.html
   ```

3. **Create `restheart.yml`** (extends base config):
   ```yaml
   /core:
     name: facet-my-example

   /ping:
     msg: My Example

   # Add example-specific configuration
   ```

4. **Create `init-data.js`** (MongoDB setup):
   ```javascript
   print('Initializing my-example data...');

   db = db.getSiblingDB('mydb');
   db.createCollection('mycollection');
   db.mycollection.insertMany([
     { name: "Item 1", value: 100 },
     { name: "Item 2", value: 200 }
   ]);

   print('Initialization complete!');
   ```

5. **Create templates** matching your URL structure

6. **Run it**:
   ```bash
   docker-compose -f ../docker-compose.yml up
   ```

## Tips and Best Practices

### Template Organization

- Use `layout.html` for shared structure (navigation, footer)
- Create `_fragments/` for reusable components and HTMX targets
- Match template paths to URL structure for intuitive resolution

### URL Design

RESTHeart exposes MongoDB resources as URLs:
- `/mydb` → database
- `/mydb/mycollection` → collection (list)
- `/mydb/mycollection/123` → document

Facet templates map directly to these URLs:
- `templates/mydb/mycollection/index.html` → collection list page
- `templates/mydb/mycollection/view.html` → document detail page

### Authentication

For examples requiring authentication:

1. Enable login service in `restheart.yml`:
   ```yaml
   /login-service:
     enabled: true
     uri: /login
   ```

2. Create `users.yml` with user accounts and permissions

3. Use `username` and `roles` variables in templates:
   ```html
   {% if roles and 'admin' in roles %}
     <!-- Admin-only content -->
   {% endif %}
   ```

### HTMX Integration

For dynamic updates without page reloads:

1. Add HTMX to layout:
   ```html
   <script src="https://unpkg.com/htmx.org@1.9.10"></script>
   ```

2. Create fragment templates in `_fragments/`:
   ```html
   {% parallel %}
   <div id="my-component">
     <!-- Component content -->
   </div>
   {% endparallel %}
   ```

3. Use HTMX attributes on links/forms:
   ```html
   <a href="/api/endpoint"
      hx-get="/api/endpoint"
      hx-target="#my-component">Update</a>
   ```

## Resources

- [Facet Developer's Guide](../docs/DEVELOPERS_GUIDE.md)
- [Template Context Reference](../docs/TEMPLATE_CONTEXT_REFERENCE.md)
- [Product Catalog Tutorial](../docs/TUTORIAL_PRODUCT_CATALOG.md)
- [RESTHeart Documentation](https://restheart.org/docs/)
- [Pebble Templates](https://pebbletemplates.io/)
- [HTMX Documentation](https://htmx.org/)

## Troubleshooting

### Port already in use

If ports 8080 or 27017 are already in use:

```bash
# Find process using port
lsof -i :8080
lsof -i :27017

# Kill process or change port in docker-compose.yml
```

### Plugin not found

Ensure you've built the Facet core plugin:

```bash
cd /path/to/facet
mvn -pl core package -DskipTests
ls -la core/target/facet-core.jar
```

The plugin must exist at `core/target/facet-core.jar` for Docker Compose to mount it.

### Templates not updating

Check that:
1. Template caching is disabled in `shared/restheart-base.yml`:
   ```yaml
   /pebble-template-processor:
     cache-active: false
   ```
2. Templates directory is correctly mounted in `docker-compose.yml`
3. You're editing files in the example's `templates/` directory

### Database reset

To reset MongoDB data to initial state:

```bash
docker-compose -f ../docker-compose.yml down -v
docker-compose -f ../docker-compose.yml up
```

This removes the data volume and re-runs `init-data.js`.

## Contributing Examples

Have an example idea? Contributions welcome!

1. Create example following the structure above
2. Include comprehensive README
3. Add sample data in `init-data.js`
4. Test with `docker-compose up`
5. Submit PR with example added to this README

Good example topics:
- Blog with comments
- Task management
- User profiles and settings
- Real-time dashboard
- API with documentation
- Multi-tenant application
