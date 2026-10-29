---
type: Guide
title: Facet Quickstart
description: Entry point for the Facet code wiki — a RESTHeart plugin that renders MongoDB data as server-side HTML via convention-based Pebble templates with first-class HTMX support.
tags: [quickstart, facet, entrypoint, restheart, mongodb]
sources:
  - id: openwiki-source-ca6cb4b1a14fd7969dfae3ec
    resource: repo://CHANGELOG.md
  - id: openwiki-source-4953616e2e42dce9b871023a
    resource: repo://core/pom.xml
  - id: openwiki-source-2273ce38809c3a26ed2bedda
    resource: repo://core/src/main/java/org/facet/html/handlers/MongoHtmlResponseHandler.java
  - id: openwiki-source-ba055822b509e50a40b16d36
    resource: repo://core/src/main/java/org/facet/html/HtmlAuthRedirectInterceptor.java
  - id: openwiki-source-24f507ac229139f88a25eaea
    resource: repo://core/src/main/java/org/facet/html/HtmlResponseInterceptor.java
  - id: openwiki-source-4398d873f3918d016977e46f
    resource: repo://core/src/main/java/org/facet/html/LoginService.java
  - id: openwiki-source-85f08b39af893d538ccc6eff
    resource: repo://core/src/main/java/org/facet/templates/PathBasedTemplateResolver.java
  - id: openwiki-source-b79fbbd921df689b4bbdc82f
    resource: repo://docker-compose.yml
  - id: openwiki-source-bb1ebe868e35e9e500714501
    resource: repo://Dockerfile
  - id: openwiki-source-47d02fca3524898d5aae2b3b
    resource: repo://LICENSE
  - id: openwiki-source-2355f81d7cf522f8dbdaabd4
    resource: repo://pom.xml
  - id: openwiki-source-23775c3de52f3ab95a13cb8b
    resource: repo://README.md
  - id: openwiki-source-227ce050fb6511ac078080da
    resource: repo://templates/mydb/products/list.html
generated: { by: "openwiki/0.7.2", at: "2026-10-29T12:45:43.106Z" }
verified:
  - by: openwiki/0.7.2
    at: 2026-10-29T12:45:43.106Z
---

# Facet Quickstart

**Facet** is a data-driven web framework that turns [MongoDB](https://mongodb.com) data into server-rendered HTML. It runs as a [RESTHeart](https://restheart.org) plugin and uses convention-based [Pebble](https://pebbletemplates.io) templates: place a template where the URL expects it, and that endpoint immediately serves HTML to browsers while continuing to return JSON to API clients.

The core idea: **your API structure is your site structure**.

```mermaid
flowchart TD
    Browser["Browser Request"]
    RESTHeart["RESTHeart API"]
    Facet["Facet Interceptor"]
    Template["Template Lookup"]
    HTML["HTML Response"]
    JSON["JSON Response"]

    Browser --> RESTHeart
    RESTHeart --> Facet
    Facet --> Template
    Template -->|"Template exists"| HTML
    Template -->|"No template"| JSON
```

*How a browser request becomes HTML: Facet intercepts RESTHeart responses and checks for matching templates.*

```
API:       /shop/products       → JSON
Template:  templates/shop/products/list.html
Result:    /shop/products       → HTML for browsers, JSON for API clients
```

No controllers, no routing config, no JavaScript build pipeline required.

## Run It in 30 Seconds

Choose your deployment path first:

1. Docker image (recommended)
Use the published Facet image for the fastest self-contained setup.

2. Manual plugin deployment (advanced)
Add Facet artifacts into an existing RESTHeart runtime when you already operate RESTHeart directly.

If you are evaluating Facet or starting from scratch, use Docker.

```bash
git clone https://github.com/SoftInstigate/facet.git && cd facet

# Recommended: use the published image (no Java needed)
docker compose up

# Optional: local build path (for Facet plugin changes)
mvn -pl core -am -DskipTests package
docker compose up --build
```

Open http://localhost:8080/mydb/products — login with **admin / secret**.

The root `docker-compose.yml` starts MongoDB 8.0, seeds sample products via `/etc/init-data.js`, and runs the Facet image — built `FROM softinstigate/restheart:9.12.3` — with templates from `/templates`. The base image was bumped from the 9.7.x line to 9.12.3; the plugin needed no source changes and the full quickstart flow (HTML rendering, cookie login, HTMX fragments, JSON passthrough) was re-verified against it.

## How It Works

1. RESTHeart exposes MongoDB collections as REST endpoints.
2. Facet intercepts HTTP responses at the `RESPONSE` phase.
3. If the request accepts HTML (`Accept: text/html` or HTMX headers) **and** a matching template exists, Facet renders HTML via Pebble.
4. If no template matches, the original JSON response passes through unchanged.

This is the [interceptor pattern](architecture.md) — SSR is opt-in per resource.

## Repository at a Glance

```
facet/
├── core/                           # The Facet plugin (JAR)
│   └── src/main/java/org/facet/
│       ├── html/                   # Interceptors & response handlers
│       │   ├── HtmlResponseInterceptor.java      ← main SSR interceptor
│       │   ├── HtmlErrorResponseInterceptor.java  ← early error rendering
│       │   ├── HtmlAuthRedirectInterceptor.java   ← auth redirects
│       │   ├── LoginService.java                  ← cookie-based login
│       │   ├── handlers/                          # Strategy handlers
│       │   └── internal/                          # HTMX helpers, utilities
│       └── templates/              # Template engine & resolution
│           ├── PathBasedTemplateResolver.java     ← hierarchical lookup
│           ├── TemplateContextBuilder.java         ← template variables
│           └── pebble/                            # Pebble implementation
├── templates/                      # Quickstart templates (root-level)
├── static/                         # Static assets (favicon, etc.)
├── etc/                            # RESTHeart config + seed data
├── examples/product-catalog/       # Full CRUD example with HTMX
├── docs/                           # Architecture diagram assets
├── pom.xml                         # Parent POM (Java 25, RESTHeart Commons 9.12.3)
└── Dockerfile                      # RESTHeart base (9.12.3) + Facet plugin
```

## What to Read Next

| Topic | Page | Why |
|-------|------|-----|
| **Architecture** | [architecture.md](architecture.md) | Understand the interceptor pipeline, plugin registration, and request flow |
| **Template System** | [template-system.md](template-system.md) | Learn the resolution algorithm, naming conventions, and context variables |
| **HTMX Integration** | [htmx.md](htmx.md) | Fragment resolution, partial updates, and mutation patterns |
| **JavaScript Plugins** | [javascript-plugins.md](javascript-plugins.md) | Write server-side services and interceptors in JavaScript, hot-reload, MongoDB access |
| **Operations** | [operations.md](operations.md) | Build, Docker, RESTHeart config, CI/CD, and release process |
| **Testing** | [testing.md](testing.md) | Unit tests (75) and integration tests (Testcontainers), how to run them, and known gaps |

## Key Technical Facts

- **Java 25** — set via `maven.compiler.release` in the parent POM
- **RESTHeart Commons 9.12.3** — core dependency, scoped as `provided`; version is managed centrally in the root `pom.xml` `<dependencyManagement>` and inherited by `core/pom.xml`, so `update-dependencies.sh` only needs to bump it in one place
- **Pebble 4.1.2** — template engine (Jinja2/Twig-like syntax)
- **Current version** — `1.1.0-SNAPSHOT` (first stable release was 1.0.0 on 2026-03-13)
- **License** — Apache 2.0

## Quick Reference: Template Conventions

```
GET /mydb/products      → templates/mydb/products/list.html   (collection)
GET /mydb/products/123  → templates/mydb/products/view.html   (document)
HTMX #product-list      → templates/mydb/products/_fragments/product-list.html
```

No template? JSON passes through. See [template-system.md](template-system.md) for the full resolution algorithm.

## Backlog

Remaining known limitations, from the CHANGELOG.md `[Unreleased]` and "Known Limitations" sections:

- **`HX-Reselect` response header** — not implemented (minor HTMX gap).
- **Streaming for JSON handler** — `JsonHtmlResponseHandler` loads full response into memory.
- **Count cache TTL** — MongoDB counts are cached in a bounded Caffeine cache (5 s TTL, 1000 entries, keyed by the filter JSON) inside `MongoHtmlResponseHandler`; the TTL and size are still compile-time constants, not runtime-configurable.
- **MongoDB timeout handling** — operation timeout not configurable.
- **Thread pool config for Pebble** — not yet exposed.

Resolved since the last update: the redirect query parameter name is now configurable — both `LoginService` and `HtmlAuthRedirectInterceptor` accept a `redirect-param` option (default `redirect`) and a `cookie-name` option (default `rh_auth`), and `LoginService` forwards both when it registers the redirect interceptor. The login flow also now rejects open-redirect targets: `LoginService.safeRedirect()` only accepts local absolute paths (`/...`, not `//...` or full URLs) and rejects values containing backslashes, quotes, angle brackets, backticks, or whitespace/control characters, falling back to `default-redirect` otherwise.
