# Facet Examples

This directory contains runnable examples demonstrating Facet's features. Each example is self-contained with templates, configuration, and sample data.

> ⚠️ Examples are work in progress and evolving rapidly, we are fixing them daily.

## Quick Start

### Prerequisites

- Docker and Docker Compose
- Java 25+ and Maven (only if building a local image)

### Running Any Example

1. **Optional: build Facet core plugin** (only if you want a local image):
   ```bash
   cd /path/to/facet
   mvn package -DskipTests
   ```

2. **Navigate to an example and start it**:
   ```bash
   cd examples/product-catalog
   docker compose up
   ```

   By default, each example's docker-compose file builds a local image. To use the published image instead, replace the `build:` section with:

   ```yaml
   image: softinstigate/facet:latest
   ```

   Then run `docker compose up` (no `--build`).

3. **Access the application** (check example README for specific URL)

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
- **JavaScript plugin** — RESTHeart service in JavaScript with hot-reload (no rebuild needed)

**Start here** if you're new to Facet!

[→ Product Catalog README](product-catalog/README.md)

---

## Example Structure

Each example is **self-contained** with its own Docker setup:

```
example-name/
├── README.md              # Example-specific documentation
├── Dockerfile             # Custom RESTHeart image with Facet plugin
├── docker-compose.yml     # MongoDB + Facet services
├── restheart.yml          # RESTHeart configuration
├── users.yml              # Authentication users (if needed)
├── init-data.js           # MongoDB initialization script
├── static/                # Static assets (favicon, images, CSS, JS)
│   ├── favicon.ico        # Prevents browser 401 auth popup
│   └── ...
├── plugins/               # JavaScript plugins (hot-reload, no rebuild needed)
│   └── my-plugin/
│       ├── package.json   # Declares services/interceptors to RESTHeart
│       └── my-service.mjs # JavaScript service or interceptor
└── templates/             # Facet templates
    ├── layout.html        # Base layout (optional)
    ├── resource/          # Resource-specific templates
    │   └── index.html
    └── _fragments/        # HTMX fragments
        └── component.html
```

Each example can be run independently from its own directory with simple `docker compose up` commands. By default, examples build a local image; you can switch to the published image by setting `image: softinstigate/facet:latest` in docker-compose.

## Docker Setup

Each example includes its own Docker configuration:

### Dockerfile

Builds a custom Facet image based on `softinstigate/restheart:9` with:
- **Facet core plugin** from `../../core/target/facet-core.jar`
- **Plugin dependencies** from `../../core/target/lib/*.jar`
- Development-friendly base configuration

The Dockerfile references the built artifacts from the repository root, allowing each example to package the latest Facet plugin. If you want to use the published image instead, you can skip the Dockerfile and set `image: softinstigate/facet:latest` in docker-compose.

### docker-compose.yml

Defines a multi-container setup with:
- **MongoDB 8.0** with data persistence and sample data initialization
- **Facet/RESTHeart** built from the local Dockerfile (or the published image if you replace the `build:` section)
- Volume mounts for configuration, templates, and static assets (enabling hot-reload)
- Healthchecks for service readiness
- Networking between containers

### Configuration Files

Each example's `restheart.yml` provides complete configuration including:
- MongoDB connection settings
- Facet plugin configuration (template processor, HTML interceptor)
- Authentication mechanisms (JWT, cookies, file-based users)
- Development-friendly settings: hot-reload enabled, caching disabled
- Example-specific services and ACL rules

This self-contained approach makes it easy to:
- Run examples independently without shared dependencies
- Customize Docker setup per example (different base images, plugins, etc.)
- Package examples for distribution or deployment

## Development Workflow

### Hot Reload Templates and JavaScript Plugins

Both Pebble templates and JavaScript plugins are hot-reloaded — no container restart needed:

**Templates:**
1. Edit any file under `templates/`
2. Refresh your browser
3. See changes immediately

**JavaScript plugins:**
1. Edit any `.mjs` file under `plugins/`
2. The next HTTP request picks up the new code automatically

This is one of the key advantages of JavaScript plugins over Java plugins — Java plugins require recompilation (`mvn package`) and a container restart.

### Viewing Logs

```bash
# From the example directory

# Facet/RESTHeart logs
docker-compose logs -f facet

# MongoDB logs
docker-compose logs -f mongodb

# All services
docker-compose logs -f
```

### Stopping Services

```bash
# From the example directory

# Stop and remove containers
docker-compose down

# Also remove data volumes (reset to initial state)
docker-compose down -v
```

### Accessing MongoDB

```bash
# Connect to MongoDB shell (container name varies by example)
docker exec -it facet-product-catalog-mongo mongosh

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

1. **Copy the product-catalog example** as a starting point:
   ```bash
   cd examples
   cp -r product-catalog my-example
   cd my-example
   ```

2. **Update configuration files**:
   - Edit `restheart.yml`: Change `/core/name` to `facet-my-example`
   - Edit `docker-compose.yml`: Update container names and volume names
   - Edit `README.md`: Document your example's features and URLs

3. **Create your templates** matching your URL structure:
   ```
   templates/
   └── myresource/
       └── index.html
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

5. **Run it**:
   ```bash
   docker compose up
   ```

   Use `docker compose up --build` if you are building a local image.

The Dockerfile and docker-compose.yml from product-catalog will work for most examples with minimal changes.

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
   <script src="https://unpkg.com/htmx.org@2.0.8"></script>
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
mvn package -DskipTests
ls -la core/target/facet-core.jar
```

The plugin must exist at `core/target/facet-core.jar` for Docker Compose to mount it.

### Templates not updating

Check that:
1. Template caching is disabled in your example's `restheart.yml`:
   ```yaml
   /pebble-template-processor:
     cache-active: false
   ```
2. Templates directory is correctly mounted in `docker-compose.yml`
3. You're editing files in the example's `templates/` directory

### Database reset

To reset MongoDB data to initial state:

```bash
# From the example directory
docker compose down -v
docker compose up
```

This removes the data volume and re-runs `init-data.js`.

## Contributing Examples

Have an example idea? Contributions welcome!

1. Create example following the structure above
2. Include comprehensive README
3. Add sample data in `init-data.js`
4. Test with `docker compose up`
5. Submit PR with example added to this README

Good example topics:
- Blog with comments
- Task management
- User profiles and settings
- Real-time dashboard
- API with documentation
- Multi-tenant application
