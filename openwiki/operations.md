---
type: Playbook
title: Operations & Deployment
description: How to build, configure, deploy, and release Facet — Maven build commands, Docker setup, RESTHeart configuration reference, CI/CD workflows, versioning with setversion.sh, and JitPack publishing.
tags: [operations, deployment, docker, maven, ci-cd, restheart, config]
resource: pom.xml
---

# Operations & Deployment

## Build

Facet is a Maven multi-module project. Java 25 is required. See [Testing Guide](testing.md) for test suite details.

```bash
# Full build with tests
mvn package

# Skip tests
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

## Docker

### Quickstart Stack

The root `docker-compose.yml` runs a minimal stack:

```yaml
services:
  mongodb:
    image: mongo:8.0
    volumes:
      - ./etc/init-data.js:/docker-entrypoint-initdb.d/init-data.js:ro
  facet:
    build: { context: ., dockerfile: Dockerfile }
    ports: ["8080:8080"]
    volumes:
      - ./etc/restheart.yml:/opt/restheart/etc/restheart.yml:ro
      - ./etc/users.yml:/opt/restheart/etc/users.yml:ro
      - ./templates:/opt/restheart/templates:ro
      - ./static:/opt/restheart/static:ro
```

The Dockerfile extends `softinstigate/restheart:9`:

```dockerfile
FROM softinstigate/restheart:9
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

```bash
cd examples/product-catalog
docker compose up   # builds from root Dockerfile with example-specific config
```

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

/jwtTokenManager:
  enabled: true
  ttl: 15
  srv-uri: /token

/authCookieSetter:
  enabled: true
  name: rh_auth
  ttl: 15
```

User credentials are in `etc/users.yml` (development only — plaintext passwords).

## CI/CD Workflows

### Build (`build.yml`)

Triggers on push/PR to `master` when `*.java` or `**/pom.xml` change:
- JDK 25 (Temurin), Maven cache
- `mvn -B verify`
- Skips if commit message contains `[skip ci]`

### Release (`release.yml`)

Triggers on semver tag push (e.g., `1.0.0`) or manual dispatch:
1. Builds core artifacts
2. Creates GitHub release with `facet-core-with-deps.zip` and `.tar.gz`
3. Builds and pushes Docker image to Docker Hub (`softinstigate/facet`)
4. Image tags match release version

### OpenWiki Update (`openwiki-update.yml`)

Daily scheduled run (08:00 UTC) + manual dispatch:
- Runs `openwiki code --update --print`
- Creates PR with documentation updates

## Versioning

`setversion.sh` manages Maven multi-module version updates:

```bash
# Set release version
./setversion.sh 1.0.0

# Set snapshot version
./setversion.sh 1.0.2-SNAPSHOT

# Preview changes
./setversion.sh 1.0.0 --dry-run
```

The script:
1. Validates semver format
2. Checks current version and branch
3. Updates both parent and core POM versions
4. Commits and tags (for release versions)

After running: `git push && git push --tags`

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

The `jitpack.yml` file configures the JitPack build environment.

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
