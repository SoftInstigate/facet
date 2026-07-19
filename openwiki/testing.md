---
type: Guide
title: Testing Guide
description: Facet's JUnit 5 test suite — test structure, how to run tests, what each test class covers, testing patterns, and known test gaps.
tags: [testing, junit, mockito, unit-tests, quality]
resource: core/src/test/java/org/facet/
---

# Testing Guide

## Test Suite Overview

The test suite covers core [architecture](architecture.md) and [template system](template-system.md) components.

Facet has a JUnit 5 test suite with 67+ tests covering core template resolution, HTMX detection, response headers, and template context building.

### Test Framework

| Dependency | Version | Purpose |
|------------|---------|---------|
| JUnit Jupiter | 6.0.3 | Test framework |
| Mockito | 5.23.0 | Mocking |
| Byte Buddy | 1.18.7-jdk5 | Java 25 compatibility for Mockito |

Tests run with the `byte-buddy-agent` as a Java agent to suppress Mockito self-attach warnings:

```xml
<argLine>-Dnet.bytebuddy.experimental=true -javaagent:${net.bytebuddy:byte-buddy-agent:jar}</argLine>
```

## How to Run Tests

```bash
# Run all tests
mvn -pl core test

# Run a specific test class
mvn -pl core test -Dtest=PathBasedTemplateResolverTest

# Run a specific test method
mvn -pl core test -Dtest=PathBasedTemplateResolverTest#testCollectionResolution

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

## No Packaged Integration Tests

There are **no automated integration tests** in the repository. Testing the full stack (RESTHeart + MongoDB + Facet) requires manual verification:

1. Build: `mvn -pl core package`
2. Deploy: `docker compose up --build`
3. Test in browser: `http://localhost:8080/mydb/products`
4. Test API: `curl -H "Accept: application/json" http://localhost:8080/mydb/products`
5. Test HTMX: open browser dev tools, verify fragment requests

## Known Test Gaps

- **Integration tests** — no automated end-to-end tests for the full RESTHeart + MongoDB + Facet stack
- **MongoHtmlResponseHandler** — the largest source file (~29KB) has no dedicated unit test class
- **JsonHtmlResponseHandler** — no dedicated unit tests
- **LoginService** — no tests
- **HtmlAuthRedirectInterceptor** — no tests
- **Pebble filters** — no dedicated unit tests for `BuildPathFilter`, `ParentPathFilter`, `StripTrailingSlashFilter`, `ToJsonFilter`
- **ETag caching logic** — `HtmlResponseHelper.setCachingHeaders()` not unit-tested
- **Error template resolution** — per-status-code lookup tested only indirectly

## When Adding Tests

1. Place tests in `core/src/test/java/org/facet/` mirroring the main source structure
2. Use `@ExtendWith(MockitoExtension.class)` for classes that need mocks
3. Use Mockito to mock `TemplateProcessor` and RESTHeart request/response objects
4. Test both success and edge cases (null, empty, missing)
5. Run the full suite: `mvn -pl core test`
