---
type: Guide
title: Testing Guide
description: Facet's test suite — JUnit 5 unit tests, Testcontainers integration tests, test structure, how to run tests, what each test class covers, testing patterns, and known test gaps.
tags: [testing, junit, mockito, unit-tests, integration-tests, testcontainers, quality]
resource: core/src/test/java/org/facet/
openwiki:
  roles: [testing]
  change_kinds: [test-coverage, integration-test]
  source_paths: [core/src/main/java/org/facet/]
  symbols: [JsPluginsIT, PathBasedTemplateResolverTest, HtmxRequestDetectorTest, HtmxResponseHelperTest, TemplateContextBuilderTest, HtmlResponseHelperTest, HtmlAuthRedirectInterceptorTest, LoginServiceTest, JsonHtmlResponseHandlerTest, PebbleTemplateProcessorTest]
  test_paths: [core/src/test/java/org/facet/integration/JsPluginsIT.java, core/src/test/java/org/facet/templates/PathBasedTemplateResolverTest.java, core/src/test/java/org/facet/html/internal/HtmxRequestDetectorTest.java, core/src/test/java/org/facet/html/internal/HtmxResponseHelperTest.java, core/src/test/java/org/facet/templates/TemplateContextBuilderTest.java, core/src/test/java/org/facet/html/internal/HtmlResponseHelperTest.java, core/src/test/java/org/facet/html/HtmlAuthRedirectInterceptorTest.java, core/src/test/java/org/facet/html/LoginServiceTest.java, core/src/test/java/org/facet/html/handlers/JsonHtmlResponseHandlerTest.java, core/src/test/java/org/facet/templates/pebble/PebbleTemplateProcessorTest.java]
  invariants: [Unit tests run via mvn -pl core test, Integration tests run via mvn -pl core verify requiring Docker, Integration test classes must use *IT.java suffix]
  validation_commands: [mvn -pl core test, mvn -pl core verify -Dit.test=JsPluginsIT]
verified:
  - by: openwiki/0.7.2
    at: 2026-10-29T12:45:43.106Z
sources:
  - id: openwiki-source-7a80b79a6fb3618cbfab08a2
    resource: repo://.github/workflows/build.yml
  - id: openwiki-source-4953616e2e42dce9b871023a
    resource: repo://core/pom.xml
  - id: openwiki-source-47278839aff3a331b532591a
    resource: repo://core/src/main/java/org/facet/html/handlers/JsonHtmlResponseHandler.java
  - id: openwiki-source-2273ce38809c3a26ed2bedda
    resource: repo://core/src/main/java/org/facet/html/handlers/MongoHtmlResponseHandler.java
  - id: openwiki-source-ba055822b509e50a40b16d36
    resource: repo://core/src/main/java/org/facet/html/HtmlAuthRedirectInterceptor.java
  - id: openwiki-source-4398d873f3918d016977e46f
    resource: repo://core/src/main/java/org/facet/html/LoginService.java
  - id: openwiki-source-7d48c89ca97482e526439e71
    resource: repo://core/src/main/java/org/facet/templates/pebble/BuildPathFilter.java
  - id: openwiki-source-c61da978692952d84db8857f
    resource: repo://core/src/main/java/org/facet/templates/pebble/ParentPathFilter.java
  - id: openwiki-source-74e4932b794ad53379967ba9
    resource: repo://core/src/main/java/org/facet/templates/pebble/StripTrailingSlashFilter.java
  - id: openwiki-source-5ac246ed102a77461bba5d27
    resource: repo://core/src/main/java/org/facet/templates/pebble/ToJsonFilter.java
  - id: openwiki-source-db8fdf8473c785bfc17a4406
    resource: repo://core/src/test/java/org/facet/html/handlers/JsonHtmlResponseHandlerTest.java
  - id: openwiki-source-96989dc2697d1e962f16f95d
    resource: repo://core/src/test/java/org/facet/html/HtmlAuthRedirectInterceptorTest.java
  - id: openwiki-source-1bb61c708a401c02eecee2b4
    resource: repo://core/src/test/java/org/facet/html/internal/HtmlResponseHelperTest.java
  - id: openwiki-source-4da15b7fab726415b863af70
    resource: repo://core/src/test/java/org/facet/html/internal/HtmxRequestDetectorTest.java
  - id: openwiki-source-fece4be56a92f6e67170e0e2
    resource: repo://core/src/test/java/org/facet/html/internal/HtmxResponseHelperTest.java
  - id: openwiki-source-01ee72995800ef5e37fedba4
    resource: repo://core/src/test/java/org/facet/html/LoginServiceTest.java
  - id: openwiki-source-e6dc9d1b51c8c391877051b7
    resource: repo://core/src/test/java/org/facet/integration/JsPluginsIT.java
  - id: openwiki-source-7b0e1f6040806b11e0d26358
    resource: repo://core/src/test/java/org/facet/templates/PathBasedTemplateResolverTest.java
  - id: openwiki-source-1a0dabaf41f64e84e9c47a5a
    resource: repo://core/src/test/java/org/facet/templates/pebble/PebbleTemplateProcessorTest.java
  - id: openwiki-source-da019c93d924bf54b626e33f
    resource: repo://core/src/test/java/org/facet/templates/TemplateContextBuilderTest.java
  - id: openwiki-source-bb1ebe868e35e9e500714501
    resource: repo://Dockerfile
  - id: openwiki-source-2355f81d7cf522f8dbdaabd4
    resource: repo://pom.xml
generated: { by: "openwiki/0.7.2", at: "2026-10-29T12:45:43.106Z" }
---

# Testing Guide

## Test Suite Overview

The test suite covers core [architecture](architecture.md) and [template system](template-system.md) components.

Facet has a JUnit 5 unit test suite with **105 `@Test` methods** across 9 non-`*IT.java` classes under `core/src/test/java/org/facet/`, covering template resolution, HTMX detection, response headers, template context building, HTML response-handler precedence rules, the Pebble template processor, login-redirect URL building, and the open-redirect guard in `LoginService`. A separate Testcontainers-based integration test class (`JsPluginsIT`, 8 tests) exercises a real RESTHeart + MongoDB stack.

### Test Framework

| Dependency | Version | Purpose |
|------------|---------|---------|
| JUnit Jupiter | 6.1.3 | Test framework |
| Mockito | 5.23.0 | Mocking |
| Byte Buddy | 1.18.11-jdk5 | Java 25 compatibility for Mockito |
| REST-Assured | 6.0.1 | HTTP API integration testing |
| Testcontainers | 2.0.5 | Docker-based integration test containers |
| Testcontainers JUnit 5 integration | 1.21.4 | `@Testcontainers`/`@Container` JUnit 5 support (`org.testcontainers:junit-jupiter`) |

These test-dependency **versions** are declared as properties directly in [`core/pom.xml`](../core/pom.xml) (`junit-jupiter.version`, `mockito-core.version`, `bytebuddy.version`, `rest-assured.version`, `testcontainers.version`, `testcontainers-junit-jupiter.version`) — they are not inherited from the root `pom.xml`'s `dependencyManagement`, which only centrally manages `restheart-commons` and `pebble`. What *does* flow from the parent pom is the **Surefire and Failsafe plugin versions**, pinned once in the root `pom.xml`'s `pluginManagement` (`maven-surefire-plugin.version` / `maven-failsafe-plugin.version`, both `3.5.6`) so both modules stay in lockstep; the actual `argLine` configuration that loads the Byte Buddy agent is still declared in `core/pom.xml`'s own `maven-surefire-plugin`/`maven-failsafe-plugin` blocks:

```xml
<argLine>-Dnet.bytebuddy.experimental=true -javaagent:${net.bytebuddy:byte-buddy-agent:jar}</argLine>
```

This argLine is applied identically to both the Surefire (`test` phase, unit tests) and Failsafe (`verify` phase, `*IT.java` integration tests) plugin configurations, and suppresses Mockito's self-attach warnings on modern JDKs.

## How to Run Tests

```bash
# Run unit tests only
mvn -pl core test

# Run a specific unit test class
mvn -pl core test -Dtest=PathBasedTemplateResolverTest

# Run a specific test method
mvn -pl core test -Dtest=PathBasedTemplateResolverTest#testCollectionResolution

# Run integration tests (requires Docker)
mvn -pl core verify -Dit.test=JsPluginsIT

# Run all tests including integration
mvn -pl core verify

# Skip tests during build
mvn -pl core -DskipTests package
```

## Test Classes

### PathBasedTemplateResolverTest (29 tests)

Covers the template resolution algorithm — the most critical component.

**What it tests:**
- Collection resolution: `list.html` → `index.html` → parent fallback chain
- Document resolution: `view.html` → `index.html` → parent fallback chain
- Document-specific override: `products/123/view.html`
- Fragment resolution: resource-specific → root fallback
- Fragment missing → empty Optional
- Hierarchical fallback with multiple parent levels

Source: [`core/src/test/java/org/facet/templates/PathBasedTemplateResolverTest.java`](../core/src/test/java/org/facet/templates/PathBasedTemplateResolverTest.java)

### HtmxRequestDetectorTest (14 tests)

Covers [HTMX](htmx.md) request detection via HTTP headers.

**What it tests:**
- `HX-Request: true` detection
- `HX-Target` retrieval (with and without `#` prefix)
- `isTargeting()` method for specific element IDs
- Edge cases: null headers, empty values, case sensitivity

Source: [`core/src/test/java/org/facet/html/internal/HtmxRequestDetectorTest.java`](../core/src/test/java/org/facet/html/internal/HtmxRequestDetectorTest.java)

### HtmxResponseHelperTest (12 tests)

Covers server-side [HTMX](htmx.md) response header methods.

**What it tests:**
- `triggerEvent()`, `triggerEventAfterSwap()`, `triggerEventAfterSettle()`
- `retarget()`, `reswap()`
- `pushUrl()`, `replaceUrl()`
- `redirect()`, `refresh()`
- Event map serialization (JSON format)

Source: [`core/src/test/java/org/facet/html/internal/HtmxResponseHelperTest.java`](../core/src/test/java/org/facet/html/internal/HtmxResponseHelperTest.java)

### TemplateContextBuilderTest (12 tests)

Covers template context building and variable injection.

**What it tests:**
- Authenticated user context (`isAuthenticated`, `username`, `roles`)
- Unauthenticated context (null values)
- `requestMethod` injection
- Custom key-value pairs
- Service data merging
- Global context preservation

Source: [`core/src/test/java/org/facet/templates/TemplateContextBuilderTest.java`](../core/src/test/java/org/facet/templates/TemplateContextBuilderTest.java)

### HtmlResponseHelperTest (15 tests)

Covers utility methods in `HtmlResponseHelper`, organized into three `@Nested` groups.

**What it tests:**
- `isEventStreamRequest()` (5 tests) — true for `text/event-stream` with or without a `charset` parameter, false for `text/html`, `*/*`, or a missing `Accept` header
- `acceptsHtml()` SSE regression (3 tests) — SSE requests must **not** be treated as HTML-capable even with a wildcard `Accept`, while plain `text/html` and HTMX (`HX-Request` + `Accept: */*`) requests still are
- `renderErrorHtml()` (7 tests) — falls back to the built-in error page when no processor or no matching template exists; renders a status-specific template (`errors/404`) when present; falls back to the generic `error` template when no specific one exists; falls back to the built-in page when `process()` throws either `TemplateProcessingException` or an unchecked `RuntimeException`; and the context passed to the template contains `statusCode`, `statusMessage`, `path`, and `username`

Source: [`core/src/test/java/org/facet/html/internal/HtmlResponseHelperTest.java`](../core/src/test/java/org/facet/html/internal/HtmlResponseHelperTest.java)

### HtmlAuthRedirectInterceptorTest (10 tests)

Covers the static URL-building and URL-parsing helpers used by `HtmlAuthRedirectInterceptor` when it redirects an unauthenticated browser request to the configured login page, rather than exercising the interceptor's full request-handling path.

**What it tests:**
- `buildLoginRedirectUrl()` — builds `/login?redirect=<encoded>`; encodes path **and** query string together as a single opaque value so the result is never double-encoded; treats a `null` and an empty query the same; appends with `&` instead of `?` when `loginUri` already has a query string; and honors a custom redirect-parameter name (e.g. `next`)
- `localPathOf()` — extracts the raw path and query from an absolute URL (dropping scheme/host), accepts a bare relative path with no query, and returns `null` for blank, unparsable, or non-path URLs (e.g. `mailto:`)
- `encodePath()` — re-encodes reserved and non-ASCII characters (`?`, `%`, `#`, spaces, `è`) while preserving path slashes, and normalizes an empty path to `/`
- An encoded id round-trips correctly through the `redirect` query parameter without double-decoding

Source: [`core/src/test/java/org/facet/html/HtmlAuthRedirectInterceptorTest.java`](../core/src/test/java/org/facet/html/HtmlAuthRedirectInterceptorTest.java)

> The interceptor's runtime behavior — distinguishing 401 (redirect) from 403 (pass through unchanged), emitting `HX-Redirect` instead of a 302 for HTMX requests, honoring `exclude-paths`, and clearing the auth cookie on failure — is implemented in `HtmlAuthRedirectInterceptor` (see [`core/src/main/java/org/facet/html/HtmlAuthRedirectInterceptor.java`](../core/src/main/java/org/facet/html/HtmlAuthRedirectInterceptor.java)) but is **not** covered by this unit test class; see [Known Test Gaps](#known-test-gaps).

### LoginServiceTest (3 tests)

Covers `LoginService.safeRedirect(String, String)`, the open-redirect guard applied to the `redirect` query parameter before it is used as a post-login destination.

**What it tests:**
- `null` and blank (empty or whitespace) requested targets fall back to the configured default
- A local absolute path with a query string (e.g. `/mydb/products?page=2`) is returned unchanged
- A parameterized test (`@ParameterizedTest` with 12 `@ValueSource` cases) asserts that unsafe targets — protocol-relative URLs (`//evil.example`), backslash-prefixed hosts, `javascript:` URIs, header/CRLF injection attempts, and paths containing quotes, backticks, angle brackets, or spaces — all fall back to the default rather than being honored, so the login page cannot be used as an open redirector

Source: [`core/src/test/java/org/facet/html/LoginServiceTest.java`](../core/src/test/java/org/facet/html/LoginServiceTest.java)

### JsonHtmlResponseHandlerTest (2 tests)

Covers `JsonHtmlResponseHandler.buildContext()`, the fallback HTML response handler for generic JSON service responses.

**What it tests:**
- Service-response JSON fields cannot override Facet-managed template variables: a response body containing `isAuthenticated: true`, `username: "spoofed"`, and `path: "/x"` is merged with service data added *before* `withAuthenticatedUser()`/`path` in `TemplateContextBuilder`, so for an unauthenticated request the resulting context still has `isAuthenticated = false` and `path` set to the real request path, while a non-conflicting field (`foo`) and the raw JSON (`data`) pass through unchanged
- An unauthenticated request never exposes a spoofed `username` value supplied by the service payload

Source: [`core/src/test/java/org/facet/html/handlers/JsonHtmlResponseHandlerTest.java`](../core/src/test/java/org/facet/html/handlers/JsonHtmlResponseHandlerTest.java)

### PebbleTemplateProcessorTest (8 tests)

Covers `PebbleTemplateProcessor` against a real file-based Pebble loader rooted in a JUnit `@TempDir`.

**What it tests:**
- `templateExists()` — true for an existing extensionless template name, false for a missing template and for a name resolving into a missing subfolder
- `process()` — renders context variables into the template output, and wraps both a missing-template failure and a template syntax error (`{% if %}oops`) into `TemplateProcessingException` rather than leaking the underlying Pebble exception
- `getGlobalTemplateContext()` returns a defensive copy (mutating the returned map does not affect the processor's internal state) and reflects values passed to the constructor
- `addToGlobalTemplateContext()` makes a new key immediately visible in subsequent `getGlobalTemplateContext()` calls

Source: [`core/src/test/java/org/facet/templates/pebble/PebbleTemplateProcessorTest.java`](../core/src/test/java/org/facet/templates/pebble/PebbleTemplateProcessorTest.java)

## Testing Patterns

### Mocking the TemplateProcessor

Tests use Mockito to mock `TemplateProcessor` and control `templateExists()` responses:

```java
when(templateProcessor.templateExists("/products/list")).thenReturn(true);
when(templateProcessor.templateExists("/products/view")).thenReturn(false);
```

`HtmlResponseHelperTest` and `JsonHtmlResponseHandlerTest` follow the same pattern, additionally stubbing `getGlobalTemplateContext()` to return `Map.of()` so `TemplateContextBuilder` has a predictable, empty starting point.

### Mocking RESTHeart Requests

`TemplateContextBuilder` and `JsonHtmlResponseHandler` tests mock `Request<?>`/`ServiceRequest<?>` to control authentication state and request metadata:

```java
when(request.getAuthenticatedAccount()).thenReturn(account);
when(account.getPrincipal()).thenReturn(() -> "admin");
when(account.getRoles()).thenReturn(Set.of("admin"));
```

### HTMX Header Testing

HTMX tests construct `HeaderMap` instances directly:

```java
HeaderMap headers = new HeaderMap();
headers.put(Headers.ACCEPT, "*/*");
headers.put(HttpString.tryFromString("HX-Request"), "true");
headers.put(HttpString.tryFromString("HX-Target"), "#product-list");
```

### Exercising a Real Pebble Loader with `@TempDir`

Rather than mocking Pebble internals, `PebbleTemplateProcessorTest` writes real `.html` template files into a JUnit `@TempDir` and constructs an actual `PebbleTemplateProcessor` instance pointed at that directory, then calls `shutdown()` in `@AfterEach`. This exercises the real file-loader and caching code paths instead of mocking Pebble's API:

```java
Files.writeString(templatesDir.resolve("hello.html"), "Hello {{ name }}!");
processor = new PebbleTemplateProcessor(Map.of("site", "facet"), true, false, Locale.ENGLISH, templatesDir.toString());
```

### Parameterized Security/Edge-Case Tests

`LoginServiceTest` uses `@ParameterizedTest` with `@ValueSource` to assert a single safety property (fallback on unsafe redirect targets) against a batch of malicious and malformed inputs in one method, rather than one `@Test` per case.

## Integration Tests

Integration tests use [Testcontainers](https://testcontainers.org/) to spin up a full RESTHeart + MongoDB stack in Docker. They are separated from unit tests and run via Maven Failsafe (`*IT.java` naming convention).

### JsPluginsIT (8 tests)

Covers JavaScript plugin execution in a real RESTHeart container — both services and interceptors.

**What it tests:**
- **Product-stats service** — returns 200 with JSON containing expected fields (`total`, `categories`, `avgPrice`, `inStock`, `outOfStock`, `totalValue`, `minPrice`, `maxPrice`, `lowStock`)
- **HTML template rendering** — service response rendered via Facet template when `Accept: text/html`, verifying template variables are available without `document.` prefix
- **Request-logger interceptor** — adds `X-Facet-Plugin`, `X-Request-Path`, and `X-Request-Method` response headers to `/shop/` endpoints
- **Authentication** — unauthenticated requests return 401

**How it works:**

The test uses two Testcontainers on a shared `Network`: a `mongo:8.0` container started with `--replSet rs0 --bind_ip_all` (its replica set is initiated in `@BeforeAll` via `mongosh ... rs.initiate()`), and a `facet-test:latest` RESTHeart container that depends on it. The RESTHeart container is configured with `RHO` environment overrides (`mclient/connection-string`, `http-listener/host`) and receives the `product-stats` and `request-logger` plugins, the `templates/` directory, `restheart.yml`, and `users.yml` from `examples/product-catalog/` via `withCopyFileToContainer()`. Tests run in a fixed order (`@TestMethodOrder(MethodOrderer.OrderAnnotation.class)`) against the mapped RESTHeart port using REST-Assured.

Source: [`core/src/test/java/org/facet/integration/JsPluginsIT.java`](../core/src/test/java/org/facet/integration/JsPluginsIT.java)

**Prerequisites:**
- Docker running locally
- Facet JARs built: `mvn -pl core package`
- A `facet-test:latest` image available locally. The repository's root [`Dockerfile`](../Dockerfile) builds a RESTHeart + Facet image from `softinstigate/restheart:9.12.3` plus the packaged `core/target/facet-core.jar` and its runtime dependencies, but no Maven or CI step currently tags it `facet-test:latest` automatically (CI's `build.yml` only runs `mvn -B package`, not `verify`); build and tag it manually first, e.g. `docker build -t facet-test:latest .`, before running `JsPluginsIT`.
- Requires GraalJS support in the RESTHeart image for the JavaScript plugins to execute — see [Known Test Gaps](#known-test-gaps).

**Run:**
```bash
mvn -pl core verify -Dit.test=JsPluginsIT
```

The `maven-failsafe-plugin` in `core/pom.xml` (`includes: **/*IT.java`) handles integration-test discovery and lifecycle (`integration-test` and `verify` goals), but it does not build the Docker image itself.

## Known Test Gaps

- **MongoDB collection endpoints** — no integration tests for Facet's SSR of MongoDB data (only JS plugin endpoints covered by JsPluginsIT)
- **JavaScript plugins** — require a RESTHeart image with GraalJS support. Use `softinstigate/restheart-snapshot:diagnostic` or later for the test image. See [restheart#663](https://github.com/SoftInstigate/restheart/issues/663) for the full history of bugs and fixes.
- **MongoHtmlResponseHandler** — the largest source file (~29KB) has no dedicated unit test class
- **Pebble filters** — no dedicated unit tests for `BuildPathFilter`, `ParentPathFilter`, `StripTrailingSlashFilter`, `ToJsonFilter`
- **HtmlAuthRedirectInterceptor request-handling path** — `HtmlAuthRedirectInterceptorTest` only covers the static `buildLoginRedirectUrl()`/`localPathOf()`/`encodePath()` helpers; the interceptor's 401-vs-403 branching, `HX-Redirect` vs. 302 selection, `exclude-paths` matching, and auth-cookie clearing are not exercised by a dedicated test
- **ETag caching logic** — `HtmlResponseHelper.setCachingHeaders()` not unit-tested
- **Error template resolution** — per-status-code lookup is tested in `HtmlResponseHelperTest`, but only for the `errors/{status}` → `error` → built-in fallback chain, not for every status code Facet may encounter

## When Adding Tests

### Unit Tests

1. Place tests in `core/src/test/java/org/facet/` mirroring the main source structure
2. Use `@ExtendWith(MockitoExtension.class)` or plain `mock(...)` calls for classes that need mocks
3. Use Mockito to mock `TemplateProcessor` and RESTHeart request/response objects
4. Test both success and edge cases (null, empty, missing)
5. Run the full suite: `mvn -pl core test`

### Integration Tests

1. Place integration tests in `core/src/test/java/org/facet/integration/`
2. Name test classes with `*IT.java` suffix (required by maven-failsafe-plugin)
3. Use `@Testcontainers` and `@Container` annotations for Docker lifecycle
4. Use REST-Assured for HTTP assertions (`given()...when()...then()`)
5. Build the Facet JAR first: `mvn -pl core package`
6. Run integration tests: `mvn -pl core verify -Dit.test=YourTestIT`
