---
type: Playbook
title: Operations & Deployment
description: How to build, configure, deploy, and release Facet — Maven build commands, Docker setup, RESTHeart configuration reference, CI/CD workflows, versioning with setversion.sh, and JitPack publishing.
tags: [operations, deployment, docker, maven, ci-cd, restheart, config]
verified:
  - by: openwiki/0.5.1
    at: 2026-09-12T23:09:54.715Z
sources:
  - id: openwiki-source-4d1d392666be6dfdd7a91a2e
    resource: repo://.github/workflows/release.yml
  - id: openwiki-source-4953616e2e42dce9b871023a
    resource: repo://core/pom.xml
  - id: openwiki-source-b8b6c47f4353d31d5b9b5b9c
    resource: repo://core/src/assembly/with-deps.xml
  - id: openwiki-source-b79fbbd921df689b4bbdc82f
    resource: repo://docker-compose.yml
  - id: openwiki-source-bb1ebe868e35e9e500714501
    resource: repo://Dockerfile
  - id: openwiki-source-10729bcc248e38025f4c362d
    resource: repo://etc/restheart.yml
  - id: openwiki-source-4d027e04a808e21feced8097
    resource: repo://examples/product-catalog/docker-compose.yml
  - id: openwiki-source-188d7998f843273008bb0d28
    resource: repo://examples/product-catalog/Dockerfile
  - id: openwiki-source-24dd11bc6e088b225824ce40
    resource: repo://jitpack.yml
  - id: openwiki-source-2355f81d7cf522f8dbdaabd4
    resource: repo://pom.xml
  - id: openwiki-source-9a509e4f28155fbfb293d571
    resource: repo://setversion.sh
generated: { by: "openwiki/0.5.1", at: "2026-09-12T23:09:54.715Z" }
---

# Operations & Deployment

## Deployment Paths

Facet supports two deployment paths:

1. **Docker image (recommended)**
Run the published `softinstigate/facet` image for the fastest and most self-contained setup.

2. **Manual plugin deployment (advanced)**
Add Facet artifacts to an existing RESTHeart installation when you need full control over that runtime.

If you are evaluating Facet or starting a new setup, use Docker first.

## Build

Facet is a Maven multi-module project. Java 25 is required. See [Testing Guide](testing.md) for test suite details.

```bash
# Full build with unit tests
mvn package

# Build and run integration tests (requires Docker)
mvn verify

# Skip all tests
mvn -DskipTests package

# Build only the core module
mvn -pl core package

# Build core and its dependencies
mvn -pl core -am package
```

The build produces:
- `core/target/facet-core.jar` — the plugin JAR
- `core/target/lib/` — runtime dependencies (Pebble, etc.)
- `core/target/facet-core-with-deps.zip` / `.tar.gz` — bundled archive for releases

For manual plugin deployment, use the bundled release archive (or the JAR together with `target/lib`).

## Docker

### Quickstart Stack

The root `docker-compose.yml` runs a minimal stack with health checks, a dedicated bridge network, and a persistent MongoDB volume:

```yaml
services:
  mongodb:
    image: mongo:8.0
    container_name: facet-mongo
    volumes:
      - mongo-data:/data/db
      - ./etc/init-data.js:/docker-entrypoint-initdb.d/init-data.js:ro
    healthcheck:
      test: echo 'db.runCommand("ping").ok' | mongosh localhost:27017/test --quiet
      interval: 10s
      timeout: 5s
      retries: 5

  facet:
    build:
      context: .
      dockerfile: Dockerfile
    image: facet-quickstart:latest
    container_name: facet
    depends_on:
      mongodb:
        condition: service_healthy
    ports:
      - "8080:8080"
    environment:
      RHO: >
        /mclient/connection-string->"mongodb://mongodb";
        /pebble-template-processor/enabled->true;
        /http-listener/host->"0.0.0.0";
    volumes:
      - ./etc/restheart.yml:/opt/restheart/etc/restheart.yml:ro
      - ./etc/users.yml:/opt/restheart/etc/users.yml:ro
      - ./templates:/opt/restheart/templates:ro
      - ./static:/opt/restheart/static:ro
    healthcheck:
      test: curl -f http://localhost:8080/ping || exit 1
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
```

The Dockerfile extends `softinstigate/restheart:9.7`:

```dockerfile
FROM softinstigate/restheart:9.7

COPY core/target/facet-core.jar /opt/restheart/plugins/
COPY core/target/lib/*.jar /opt/restheart/plugins/

CMD ["-o", "/opt/restheart/etc/restheart.yml"]
```

### Run with Published Image

```bash
docker run --rm -p 8080:8080 \
  -v "$PWD/etc/restheart.yml:/opt/restheart/etc/restheart.yml:ro" \
  -v "$PWD/etc/users.yml:/opt/restheart/etc/users.yml:ro" \
  -v "$PWD/templates:/opt/restheart/templates:ro" \
  -v "$PWD/static:/opt/restheart/static:ro" \
  softinstigate/facet:latest -o /opt/restheart/etc/restheart.yml
```

### Product Catalog Example

The product-catalog example extends the published `softinstigate/facet:latest` image with example-specific configuration and JavaScript plugins:

```bash
cd examples/product-catalog
docker compose up   # builds from root Dockerfile with example-specific config
```

The example mounts additional JavaScript plugins (`product-stats`, `request-logger`) into `/opt/restheart/plugins/` for hot-reload during development. See the example's `docker-compose.yml` for the full volume configuration.

## RESTHeart Configuration Reference

Key configuration blocks in `restheart.yml`:

### Pebble Template Processor

```yaml
/pebble-template-processor:
  enabled: true
  use-file-loader: true
  cache-active: false           # true for production
  templates-path: /opt/restheart/templates
```

### HTML Response Interceptor

See [Architecture](architecture.md) for how this interceptor fits into the request flow.

```yaml
/html-response-interceptor:
  enabled: true
  response-caching: false       # true for production (ETag caching)
  max-age: 5                    # Cache-Control max-age (seconds)
```

### Auth Redirect Interceptor

```yaml
/html-auth-redirect-interceptor:
  enabled: true
  login-uri: /login
```

### Login Service

```yaml
/login-service:
  enabled: true
  uri: /login
  redirect-param: redirect
  default-redirect: /
  roles-endpoint: /roles
```

### MongoDB Client

```yaml
/mclient:
  connection-string: "mongodb://mongodb"

/mongo/mongo-mounts:
  - what: "*"
    where: /
```

### Authentication

```yaml
/basicAuthMechanism:
  enabled: true
  authenticator: fileRealmAuthenticator

/fileRealmAuthenticator:
  enabled: true
  conf-file: /opt/restheart/etc/users.yml
```

### JWT Token Management

```yaml
/jwtTokenManager:
  enabled: true
  ttl: 15                       # Token lifetime in minutes
  srv-uri: /token

/jwtConfigProvider:
  key: change-me                # Secret key — change for production
  algorithm: HS256
  base64Encoded: false
  issuer: facet-quickstart

/jwtAuthenticationMechanism:
  enabled: true
  base64Encoded: false
  usernameClaim: sub
  rolesClaim: roles
  fixedRoles: []
```

### Auth Cookies

```yaml
/authCookieSetter:
  enabled: true
  name: rh_auth
  domain: localhost             # Set to your domain for production
  path: /
  secure: false                 # true for HTTPS
  http-only: true
  same-site: true
  same-site-mode: lax
  ttl: 15                       # Minutes — matches jwtTokenManager/ttl
  allow-legacy: true

/authCookieHandler:
  enabled: true

/authCookieRemover:
  enabled: true
  secure: false                 # true for HTTPS
  uri: /logout
```

### Authorization (File ACL)

```yaml
/fileAclAuthorizer:
  enabled: true
  permissions:
    # Unauthenticated users can access static assets
    - roles: ['$unauthenticated']
      predicate: >
        path('/favicon.ico') or path-prefix('/apple-touch-icon')
        or path('/robots.txt') or path-prefix('/static')
      priority: 999
      mongo:
        allowManagementRequests: false
        allowBulkPatch: false
        allowBulkDelete: false
        allowWriteMode: false
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

### Static Resources

```yaml
/static-resources:
  - what: /opt/restheart/static/favicon.ico
    where: /favicon.ico
    embedded: false
  - what: /opt/restheart/static
    where: /static
    embedded: false
```

User credentials are in `etc/users.yml` (development only — plaintext passwords).

## CI/CD Workflows

### Build (`build.yml`)

Triggers on push/PR to `master` when `*.java` or `**/pom.xml` change:
- JDK 25 (Temurin), Maven cache
- `mvn -B package`
- Skips if commit message contains `[skip ci]`

### Release (`release.yml`)

Triggers on semver tag push (e.g., `1.0.0`) or manual dispatch:
1. Builds core artifacts (`mvn -B package`)
2. Creates GitHub release with `facet-core-with-deps.zip` and `.tar.gz` (tag push only)
3. Builds and pushes Docker image to Docker Hub (`softinstigate/facet`)
4. Image tags match release version plus `latest`

### Release Notes Template

Use this snippet in each release to keep artifact communication consistent:

```markdown
## Distribution

- Primary artifact: `softinstigate/facet:<version>` Docker image (recommended default)
- Secondary artifact: Facet plugin bundle (`facet-core-with-deps.zip` and `.tar.gz`) for manual RESTHeart deployments

## Compatibility

- Supported RESTHeart line: 9.x
- See this release assets for exact artifact versions and checksums
```

Keep the Docker image line first in release notes so new users see the default path immediately.

### OpenWiki Update (`openwiki-update.yml`)

Weekly scheduled run (Saturdays 04:17 UTC) + manual dispatch:
- Runs `openwiki code --update --print`
- Creates PR with documentation updates to `openwiki/` and agent files

## Versioning

`setversion.sh` manages Maven multi-module version updates:

```bash
# Set release version
./setversion.sh 1.0.0

# Set snapshot version
./setversion.sh 1.0.3-SNAPSHOT

# Preview changes
./setversion.sh 1.0.0 --dry-run

# Force version change (e.g., downgrade or overwrite existing tag)
./setversion.sh 1.0.2 --force
```

The script:
1. Validates semver format
2. Checks current version and branch (requires `master`, `release/*`, or `<major>.x`)
3. Verifies working tree is clean
4. Updates both parent and core POM versions
5. Commits and tags (for release versions)

After running: `git push && git push --tags`

### Dependency Updates

`update-dependencies.sh` uses the Maven Versions Plugin to update dependency versions:

```bash
# Patch-level updates only (default)
./update-dependencies.sh

# Allow minor updates
./update-dependencies.sh true
```

The script filters out prerelease qualifiers (M, RC, alpha, beta, EA) by default. Override with the `MAVEN_VERSION_IGNORE` environment variable:

```bash
MAVEN_VERSION_IGNORE=".+-M[0-9]*" ./update-dependencies.sh
```

All dependency versions in both POMs are managed via `<properties>` for centralized control. The parent POM owns shared build-plugin versions; `core/pom.xml` owns test-scoped dependency versions.

## Maven / JitPack Distribution

Facet publishes release tags to [JitPack](https://jitpack.io/#SoftInstigate/facet):

```xml
<repository>
  <id>jitpack</id>
  <url>https://jitpack.io</url>
</repository>

<dependency>
  <groupId>com.github.SoftInstigate</groupId>
  <artifactId>facet-core</artifactId>
  <version>1.0.0</version>
</dependency>
```

The `jitpack.yml` file configures the JitPack build environment (currently `openjdk25`).

## Development Workflow

1. Make changes to Java source in `core/src/main/java/`
2. Run tests: `mvn -pl core test`
3. Build: `mvn -pl core -am package`
4. Restart Docker stack (or use `docker compose up --build`)
5. Edit templates in `/templates` — hot-reload works when `cache-active: false`
6. Test in browser at `http://localhost:8080/`

### RESTHeart Environment Override

Docker Compose uses the `RHO` environment variable to override config at runtime:

```yaml
environment:
  RHO: >
    /mclient/connection-string->"mongodb://mongodb";
    /pebble-template-processor/enabled->true;
    /http-listener/host->"0.0.0.0";
```

## When Changing Plugin Behavior

| Change | File(s) to Edit | Test |
|--------|-----------------|------|
| Modify template resolution | `PathBasedTemplateResolver.java` | `PathBasedTemplateResolverTest` |
| Add template variables | `TemplateContextBuilder.java` | `TemplateContextBuilderTest` |
| Change HTMX detection | `HtmxRequestDetector.java` | `HtmxRequestDetectorTest` |
| Add HTMX response headers | `HtmxResponseHelper.java` | `HtmxResponseHelperTest` |
| Change interceptor matching | `HtmlResponseInterceptor.java` | Manual integration test |
| Add Pebble filter | New filter + `CustomPebbleExtension.java` | Unit test |
| Change error pages | `HtmlResponseHelper.java` | `HtmlResponseHelperTest` |
| Add/modify JavaScript plugin | `examples/product-catalog/plugins/` | `JsPluginsIT` (integration test) |
