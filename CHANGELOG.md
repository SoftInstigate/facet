# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.2] - 2026-08-11

### Changed

- **RESTHeart commons** updated from 9.5.2 to 9.7.2 (compatible with RESTHeart 9.7).
- **Pebble** updated from 4.1.1 to 4.1.2.
- **Maven plugin versions** now managed as properties in the parent POM:
  `maven-compiler-plugin` 3.15.0, `maven-surefire-plugin` 3.5.6, `maven-failsafe-plugin` 3.5.6,
  `maven-dependency-plugin` 3.11.0, `maven-assembly-plugin` 3.8.0, `versions-maven-plugin` 2.21.0.
- **Dockerfile** base image updated to RESTHeart 9.7.

### Added

- **JavaScript plugin integration tests** — `JsPluginsIT` validates server-side JavaScript plugin
  loading and a request-logger interceptor using HTMX-driven verification.

### Fixed

- **`update-dependencies.sh`** improved with better version control logic.

---

## [1.0.1] - 2026-03-19

### Fixed

- Bypass HTML interception for SSE `event-stream` requests (#8).
- Allow `setversion.sh` to run from `master` and `release/*` branches.
- Update `restheart.version` to 9.2.0 in POM.

### Added

- **OpenWiki documentation** — Architecture, HTMX integration, operations, quickstart, template
  system, and testing guides now live in the repository under `openwiki/`.
- **`update-dependencies.sh`** — Script to update project dependencies using Maven Versions Plugin.
- **Polyglot extensibility** documented in README (GraalVM support).

### Changed

- README rewritten for clarity on Facet as a RESTHeart plugin with server-rendered HTML.
- CI workflows updated to latest GitHub Actions versions.
- Documentation consolidated: detailed guides moved to OpenWiki and the project website.

---

## [1.0.0] - 2026-03-13

First stable release. The core API and template conventions are now considered stable.

### Added

- **`requestMethod` template variable** — All templates now receive the HTTP method of the current
  request (`GET`, `POST`, `PATCH`, `DELETE`, `PUT`, `HEAD`, `OPTIONS`, `OTHER`). Enables
  conditional rendering for post-mutation feedback (e.g., showing "Created successfully!" after POST).

- **Per-status-code error templates** — `HtmlResponseHelper.renderErrorPage()` now looks for
  `templates/errors/{statusCode}.html` before falling back to the generic `templates/error.html`.
  Added `PathBasedTemplateResolver.resolveError()` to support this lookup. Example 404 page added
  to the product-catalog example at `templates/errors/404.html`.

- **MongoDB count caching** — `MongoHtmlResponseHandler` now caches the results of
  `estimatedDocumentCount()`, `countDocuments()`, `listDatabaseNames()`, and `listCollectionNames()`
  with a 5-second TTL using a `ConcurrentHashMap<String, CacheEntry>`. Eliminates one or more
  extra MongoDB round-trips per rendered page. Cache hits are logged at DEBUG level.

- **JUnit 5 test suite** — Added `junit-jupiter` 5.14.3 and `mockito-core` 5.23.0 (with
  `byte-buddy` 1.18.7-jdk5 for Java 25 support). New test classes:
  - `PathBasedTemplateResolverTest` — 29 tests covering collection, document, fragment, and error
    template resolution, including all hierarchical fallback branches.
  - `HtmxRequestDetectorTest` — 14 tests covering HTMX header parsing and `isTargeting()`.
  - `TemplateContextBuilderTest` — 12 tests covering auth variables, `requestMethod`, and
    context merging.
  - `HtmxResponseHelperTest` — 12 tests covering all HTMX response header methods.

- **"Handling Mutations" section in DEVELOPERS_GUIDE.md** — Documents three patterns for
  responding to form submissions and API mutations: HTMX Fragment Swap (recommended), Post-Redirect-Get,
  and inline response with `requestMethod`.

### Changed

- **Version bumped** from `0.2.0-SNAPSHOT` to `1.0.0` in `pom.xml` and `core/pom.xml`.

- **DEVELOPERS_GUIDE.md** — Version updated to `1.0.0`, date updated to `2026-03-13`.

- **README.md** — Maven/Gradle coordinates updated from `RELEASE_VERSION` placeholder to `1.0.0`.

- **TEMPLATE_CONTEXT_REFERENCE.md** — `requestMethod` variable documented in Authentication Context
  section.

---

## [0.2.0] - 2026-01

### Added

- Parametric mongo-mount support and multi-tenant context (`tenantId`, `isMultiTenant`, `hostParams`)
- Custom Pebble filters: `stripTrailingSlash`, `buildPath`, `parentPath`, `toJson`
- HTMX strict mode: missing fragment template returns 500 to surface errors early
- CRUD example in product-catalog with HTMX-powered edit forms
- `HX-Reselect` gap documented as known limitation

### Changed

- Template resolver refactored to `PathBasedTemplateResolver` with full hierarchical fallback
- `MongoHtmlResponseHandler` enriches documents with `_id` metadata for URL generation

---

## [0.1.0] - 2025

Initial release.

### Added

- Core interceptor architecture (`HtmlResponseInterceptor`, `HtmlErrorResponseInterceptor`,
  `HtmlAuthRedirectInterceptor`)
- Path-based template resolution with hierarchical fallback
- Pebble template engine integration
- HTMX fragment resolution via `HX-Target` header
- MongoDB response handler with pagination
- ETag-based response caching
- Product-catalog example with Pico CSS

---

## Known Limitations (Future Work)

These issues exist but are not blocking for 1.0.0:

- `HX-Reselect` response header not implemented (minor HTMX gap)
- `JsonHtmlResponseHandler` loads full response into memory (no streaming)
- MongoDB operation timeout handling not configurable
- Thread pool size not configurable in `PebbleTemplateProcessor`
- Redirect query param name (`?redirect=`) not configurable
- Count cache TTL is hardcoded at 5 seconds (not runtime-configurable per handler)
