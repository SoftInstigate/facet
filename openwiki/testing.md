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
  symbols: [JsPluginsIT, PathBasedTemplateResolverTest, HtmxRequestDetectorTest, HtmxResponseHelperTest, TemplateContextBuilderTest, HtmlResponseHelperTest]
  test_paths: [core/src/test/java/org/facet/integration/JsPluginsIT.java, core/src/test/java/org/facet/templates/PathBasedTemplateResolverTest.java, core/src/test/java/org/facet/html/internal/HtmxRequestDetectorTest.java, core/src/test/java/org/facet/html/internal/HtmxResponseHelperTest.java, core/src/test/java/org/facet/templates/TemplateContextBuilderTest.java, core/src/test/java/org/facet/html/internal/HtmlResponseHelperTest.java]
  invariants: [Unit tests run via mvn -pl core test, Integration tests run via mvn -pl core verify requiring Docker, Integration test classes must use *IT.java suffix]
  validation_commands: [mvn -pl core test, mvn -pl core verify -Dit.test=JsPluginsIT]
---

# Testing Guide

## Test Suite Overview

The test suite covers core [architecture](architecture.md) and [template system](template-system.md) components.

Facet has a JUnit 5 test suite with 75 tests covering core template resolution, HTMX detection, response headers, template context building, and response helper utilities.

### Test Framework

| Dependency | Version | Purpose |
|------------|---------|---------|
| JUnit Jupiter | 6.1.3 | Test framework |
| Mockito | 5.23.0 | Mocking |
| Byte Buddy | 1.18.11-jdk5 | Java 25 compatibility for Mockito |
| REST-Assured | 6.0.1 | HTTP API integration testing |
| Testcontainers | 2.0.5 | Docker-based integration test containers |

Tests run with the `byte-buddy-agent` as a Java agent to suppress Mockito self-attach warnings:

```xml
<argLine>-Dnet.bytebuddy.experimental=true -javaagent:${net.bytebuddy:byte-buddy-agent:jar}</argLine>
```

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

### HtmlResponseHelperTest

Covers utility methods in `HtmlResponseHelper`.

**What it tests:**
- `acceptsHtml()` — standard browser and HTMX header combinations
- `isEventStreamRequest()` — SSE bypass detection
- Error page rendering

Source: [`core/src/test/java/org/facet/html/internal/HtmlResponseHelperTest.java`](../core/src/test/java/org/facet/html/internal/HtmlResponseHelperTest.java)

## Testing Patterns

### Mocking the TemplateProcessor

Tests use Mockito to mock `TemplateProcessor` and control `templateExists()` responses:

```java
when(templateProcessor.templateExists("/products/list")).thenReturn(true);
when(templateProcessor.templateExists("/products/view")).thenReturn(false);
```

### Mocking RESTHeart Requests

`TemplateContextBuilder` tests mock `Request<?>` to control authentication state:

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

The test uses two Testcontainers: a MongoDB 8.0 replica set and a RESTHeart container built from the Facet JARs. The RESTHeart container is configured with `RHO` environment overrides and receives plugin/template files via `withCopyFileToContainer()`. A `@BeforeAll` method initializes the MongoDB replica set before tests run.

Source: [`core/src/test/java/org/facet/integration/JsPluginsIT.java`](../core/src/test/java/org/facet/integration/JsPluginsIT.java)

**Prerequisites:**
- Docker running locally
- Facet JARs built: `mvn -pl core package`

**Run:**
```bash
mvn -pl core verify -Dit.test=JsPluginsIT
```

The test image `facet-test:latest` is built automatically by the Maven build. The `maven-failsafe-plugin` in `core/pom.xml` handles integration test lifecycle.

## Known Test Gaps

- **MongoDB collection endpoints** — no integration tests for Facet's SSR of MongoDB data (only JS plugin endpoints covered by JsPluginsIT)
- **JavaScript plugins** — require RESTHeart image with GraalJS support. Use `softinstigate/restheart-snapshot:diagnostic` or later. See [restheart#663](https://github.com/SoftInstigate/restheart/issues/663) for the full history of bugs and fixes.
- **MongoHtmlResponseHandler** — the largest source file (~29KB) has no dedicated unit test class
- **LoginService** — no tests
- **HtmlAuthRedirectInterceptor** — no tests
- **Pebble filters** — no dedicated unit tests for `BuildPathFilter`, `ParentPathFilter`, `StripTrailingSlashFilter`, `ToJsonFilter`
- **ETag caching logic** — `HtmlResponseHelper.setCachingHeaders()` not unit-tested
- **Error template resolution** — per-status-code lookup tested only indirectly

## When Adding Tests

### Unit Tests

1. Place tests in `core/src/test/java/org/facet/` mirroring the main source structure
2. Use `@ExtendWith(MockitoExtension.class)` for classes that need mocks
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

