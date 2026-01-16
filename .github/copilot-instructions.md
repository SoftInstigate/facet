Purpose
-------
This repository contains Facet, a small server-side rendering (SSR) framework extracted for use with RESTHeart. The goal of this file is to give an AI coding agent the minimal, actionable knowledge it needs to be productive: architecture, build/test feedback loop, important conventions, and concrete examples from the codebase.

**Big picture**
- Facet is a lightweight SSR layer for RESTHeart that converts JSON/Mongo responses into HTML pages or HTMX fragments.
- Core responsibilities live in the `core` module: template resolution, template processing, HTMX support, and RESTHeart plugin interceptors.

**Key components & where to look**
- **Plugin entrypoints / interceptors**: [core/src/main/java/org/facet/html/HtmlResponseInterceptor.java](core/src/main/java/org/facet/html/HtmlResponseInterceptor.java) and [core/src/main/java/org/facet/html/HtmlErrorResponseInterceptor.java](core/src/main/java/org/facet/html/HtmlErrorResponseInterceptor.java). These files show how the project integrates with RESTHeart via `@RegisterPlugin` and `InterceptPoint`.
- **Template processing contract**: [core/src/main/java/org/facet/templates/TemplateProcessor.java](core/src/main/java/org/facet/templates/TemplateProcessor.java) (the app uses a provider named `pebble-template-processor`).
- **Template resolution strategy**: [core/src/main/java/org/facet/templates/PathBasedTemplateResolver.java](core/src/main/java/org/facet/templates/PathBasedTemplateResolver.java) — describes path-based hierarchical resolution and HTMX fragment lookup.
- **Template context helpers**: [core/src/main/java/org/facet/templates/TemplateContextBuilder.java](core/src/main/java/org/facet/templates/TemplateContextBuilder.java).
- **HTMX handling and rules**: [core/src/main/java/org/facet/html/internal/HtmxRequestDetector.java](core/src/main/java/org/facet/html/internal/HtmxRequestDetector.java) and HTMX usage in `HtmlResponseInterceptor`.
- **Error rendering & caching policy**: [core/src/main/java/org/facet/html/internal/HtmlResponseHelper.java](core/src/main/java/org/facet/html/internal/HtmlResponseHelper.java) — shows ETag generation, development caching toggle, and error fallback HTML.

Architecture & design notes an agent should know
- The framework registers as RESTHeart plugins (see `@RegisterPlugin`) and runs as interceptors at specific intercept points (`REQUEST_AFTER_AUTH`, `RESPONSE`) — changing intercept behaviour is done by editing those classes.
- Template resolution is path-driven. Full pages walk the path hierarchy (eg. `/a/b/c/index` → parent fallbacks). HTMX fragments look in `_fragments/` under the resource or the root (no hierarchical walk).
- Action-aware resolution: for Mongo requests the resolver tries `list` (collections) or `view` (documents) before falling back to `index`.
- Templates are referenced without the `.html` extension in resolver calls — Pebble adds extensions at render time.
- TemplateProcessor is injected using the component name `pebble-template-processor`. Mongo access is injected as `mclient` and RESTHeart commons are provided at runtime (scope `provided` in POM).

Build / run / debug
- Java requirement: the top-level POM sets `<maven.compiler.release>25</maven.compiler.release>` — ensure a compatible JDK is used locally.
- Standard build: run Maven at repository root. Example (skip tests if needed):

```bash
mvn -DskipTests package
```

- To build only core module:

```bash
mvn -pl core package
```

- There are no packaged integration tests in this repo; local debugging is typically done by deploying the compiled JAR(s) into a RESTHeart runtime and enabling the plugin by name (`html-response-interceptor`). The plugin config keys are shown in `HtmlResponseInterceptor` Javadoc comments:

```yaml
/html-response-interceptor:
  enabled: true
  response-caching: false  # optional - development
  max-age: 5               # seconds
```

Project-specific conventions and patterns
- Template names passed to `TemplateProcessor` are canonical (no leading `templates/` prefix in the resolver results) and do not include `.html`.
- HTMX strict-mode fragments: if an HTMX request sets `HX-Target`, fragment resolution is strict — missing fragment → 500 for HTMX path (see `HtmlResponseInterceptor` behaviour).
- Caching: ETag is computed from the rendered HTML's `hashCode()`. The helper `setCachingHeaders` returns a boolean indicating whether the caller should send 304. Development mode can disable caching via `response-caching: false`.
- Interceptor matching: the `resolve(...)` method in interceptors is the gatekeeper — modify it to change which requests get HTML rendering.

Integration points & dependencies
- Runtime dependency: RESTHeart (plugins use RESTHeart types such as `ServiceRequest`, `ServiceResponse`, `@RegisterPlugin`). The POM marks `restheart-commons` as `provided`.
- Template engine: Pebble (`pebble` dependency). Implementations are provided via a `pebble-template-processor` component.
- Mongo: handlers rely on a `mclient` injection (MongoClient) and inspect `MongoRequest`/`ResolvedContext` for canonical path resolution.

Quick examples to copy/paste
- Resolve a fragment for HTMX target `product-list` on `/mydb/mycoll`:
  - Look for `templates/mydb/mycoll/_fragments/product-list.html`, then `templates/_fragments/product-list.html`.
- To add a global template context value in startup code, call `templateProcessor.addToGlobalTemplateContext("key", value)` (see `TemplateProcessor` interface).

What an AI agent should do first when editing code
- Grep for usages of `pebble-template-processor`, `mclient`, and `@RegisterPlugin` to understand runtime wiring.
- Inspect `PathBasedTemplateResolver` when modifying rendering or fragment behavior — it's the central place for template lookup rules.
- If altering response caching, update tests (where applicable) and the logic in `HtmlResponseHelper.setCachingHeaders` and `HtmlResponseInterceptor.checkAndSendResponse` together.

Where to ask humans for clarification
- JDK compatibility target (Java 25) — confirm if a lower JDK is required for the target environment.
- Expected template directory layout used by deployment (where `templates/` are located at runtime) — needed if adding disk-based template loading.

If you'd like, I can iterate: run a repo-wide grep for the injection names, or open a draft PR adding a small README describing how to deploy the plugin into a local RESTHeart instance.
