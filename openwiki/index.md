---
type: Documentation Index
title: "OpenWiki"
description: "Files and subdirectories in OpenWiki."
---

# Files

- [Facet Architecture](architecture.md) - Core architecture of Facet — the interceptor pipeline, plugin registration via @RegisterPlugin, dependency injection, request flow from RESTHeart through template rendering, and the response handler strategy pattern.
- [HTMX Integration](htmx.md) - How Facet detects HTMX requests, resolves fragment templates, provides server-side HTMX response headers, handles SSE bypass, and supports mutation patterns with partial page updates.
- [Operations & Deployment](operations.md) - How to build, configure, deploy, and release Facet — Maven build commands, Docker setup, RESTHeart configuration reference, CI/CD workflows, versioning with setversion.sh, and JitPack publishing.
- [Facet Quickstart](quickstart.md) - Entry point for the Facet code wiki — a RESTHeart plugin that renders MongoDB data as server-side HTML via convention-based Pebble templates with first-class HTMX support.
- [Template System](template-system.md) - Facet's template resolution algorithm, naming conventions (list.html/view.html), hierarchical fallback, template context variables, Pebble custom filters, and error template support.
- [Testing Guide](testing.md) - Facet's JUnit 5 test suite — test structure, how to run tests, what each test class covers, testing patterns, and known test gaps.
