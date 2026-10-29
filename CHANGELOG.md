# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added

- `HtmlAuthRedirectInterceptor` options `redirect-param` (default `redirect`) and `cookie-name`
  (default `rh_auth`); `LoginService` option `cookie-name`, forwarded together with `redirect-param`
  to the interceptor it registers.
- `mongoPrefix` global template variable is now actually set at startup by `HtmlResponseInterceptor`
  from the `mongo-mounts` configuration, so `mongoPath` is prefix-stripped when MongoDB is mounted
  under e.g. `/api`.
- `HtmlResponseHelper.renderErrorHtml()` public helper that returns the error page HTML, or the
  built-in fallback page.

### Changed

- **RESTHeart 9.12.3** — `restheart-commons` dependency and the Docker base image
  (`softinstigate/restheart:9.12.3`) updated from 9.7.x. No source changes were needed; the plugin
  compiles unchanged and the quickstart stack was verified end-to-end (HTML rendering, cookie login,
  HTMX fragments, JSON API passthrough) on 9.12.3.
- Quickstart and product-catalog configurations now disable the MCP service (`/mcpService`), which
  RESTHeart enables by default since 9.9.
- `pebble-template-processor`'s `cache-active` now really defaults to `true` as documented (it was
  `false` unless the key was present).
- `html-response-interceptor`'s `response-caching` default is `true` also when the plugin has no
  configuration block; Javadoc fixed accordingly.
- Template resolution runs once per request: the name resolved in `resolve()` is reused by
  `handle()`. Template existence is checked through the Pebble loader without compiling the
  template.
- Template lookup logging moved from INFO/WARN to DEBUG/TRACE: no more per-request log noise for
  every template tried in the hierarchical walk.
- MongoDB count cache is now a bounded Caffeine cache (5 s TTL, 1000 entries) keyed by the filter
  JSON instead of its `hashCode()`.
- HTMX requests that fail authentication receive `HX-Redirect` to the login page and keep status
  401, instead of a 302 that HTMX would swap into the target element. The redirect target is the
  page URL from `HX-Current-URL`, not the fragment or action endpoint.
- A template that exists but fails to render now yields a 500 (error page, or plain text for HTMX)
  instead of silently sending the JSON payload with status 200. Template existence is checked
  without compiling, so syntax errors surface at render time.
- **`tenantId` template variable** is now the plain tenant id, or `null` when none was resolved,
  instead of a JSON-quoted string (`"tenant"`) or the literal string `"null"`. Templates that
  embedded it in JavaScript should use `{{ tenantId | toJson }}`; `{% if tenantId %}` now works.
- `ETag` of rendered pages is a 128-bit SHA-256 prefix instead of the 32-bit `String.hashCode()`,
  which could return 304 Not Modified for content that had actually changed.
- Pebble's rendering thread pool (used only by the `{% parallel %}` tag) uses named daemon threads.

### Fixed

- **`update-dependencies.sh`** no longer applies major updates when run with `true`: the Versions
  Plugin defaults `allowMajorUpdates` to `true`, so it is now disabled explicitly. The prerelease
  ignore patterns also match versions such as `4.0.0-beta-5`.
- Version properties defined in the parent POM but used only in `core` (`restheart-commons`,
  Surefire, Failsafe) were never updated by the script. The parent POM now declares them in
  `dependencyManagement`/`pluginManagement` and `core` inherits the managed versions.
- **`update-dependencies.sh`** improved with better version control logic.
- **`setversion.sh`** stages only the POM files it changes instead of `git add -A`, so unrelated
  uncommitted work is no longer swept into the version bump commit.
- Error pages returned HTTP 500 with an empty body when no `templates/error.html` (or
  `errors/{code}.html`) existed: Pebble's unchecked `LoaderException` escaped the renderer and the
  built-in fallback page was never used. `PebbleTemplateProcessor.process()` now wraps every Pebble
  exception in `TemplateProcessingException`, and the error renderer falls back to the built-in page
  when no template is found.
- The auth redirect `Location` lost the original query string and did not URL-encode the path; it
  also cleared the cookie with a hardcoded name.
- `JsonHtmlResponseHandler` let fields of the service JSON response (e.g. `username`,
  `isAuthenticated`, `path`) overwrite the variables set by Facet. Facet's own variables
  (`isAuthenticated`, `username`, `roles`, `requestMethod`, `path`, `data`) now always win; a
  service that needs a field with one of those names must rename it.
- `HtmlAuthRedirectInterceptor` also fired for authenticated users that were merely not authorized
  (403), clearing their auth cookie; it now acts only on unauthenticated requests. The redirect
  parameter is built from the raw request URI, so encoded characters in document ids survive.
- `mongoPath` prefix stripping now respects segment boundaries (`/api` no longer strips
  `/apidocs`) and the prefix is normalized (no trailing slash).
- `hostParams` JSON was built by string concatenation without escaping.

### Security

- `login-service` no longer acts as an open redirector: the `redirect` query parameter is honored
  only for local absolute paths (`/...`, not `//...` or absolute URLs); anything else falls back to
  `default-redirect`. Values containing backslashes, quotes, angle brackets, backticks or
  whitespace are rejected too, so the value cannot break out of the JavaScript string in the
  login template; the bundled login templates now also apply `escape(strategy="js")`.

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
