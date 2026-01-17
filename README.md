# <img src="https://getfacet.org/assets/img/facet-logo.svg" alt="Facet logo" width="32px" height="auto" /> Facet

## The data-driven Web framework - One API, many facets

Decorate your APIs with HTML using path-based templates.
No backend coding required.

<img src="docs/decorate.png" alt="decorate API" width="98%" height="auto" />

## What is Facet?

Facet is a data-driven web framework that transforms **JSON** documents into **server-rendered HTML** through convention-based templates, providing a hybrid approach where HTML is just another representation of your API.

## Benefits

- **Zero Backend Code:** Ship features without writing controllers or services. Templates + JSON documents = HTML, end to end.
- **Hybrid by Default:** API and UI from the same endpoint. JSON for mobile apps, HTML for web. Zero duplication.
- **Convention-Based:** Drop a template where the path matches the resource and you're done. No routing files.
- **Progressive Enhancement:** Start with fast SSR, sprinkle HTMX for interactivity. SEO-friendly, minimal JS, smooth UX.
- **Direct Data Binding:** Automatically maps APIs to HTML using path-based templates.
- **Content Negotiation:** Browsers get HTML, API clients get JSON - same endpoint, no extra wiring.

## Features

- **Path-based template resolution** - Templates mirror your API structure
- **HTMX support** - First-class support for partial updates and progressive enhancement
- **Pebble templates** - Clean syntax inspired by Twig/Jinja2
- **Generic handlers** - Works with MongoDB and JSON responses out of the box
- **Production-ready** - Built as a RESTHeart plugin on GraalVM
- **Framework agnostic** - Use any CSS/JS framework (React, Vue, Alpine.js, vanilla JS)

## Built On Proven Technologies

- **[RESTHeart](https://restheart.org)** - High-performance MongoDB REST API server
- **[Pebble](https://pebbletemplates.io)** - Fast, lightweight template engine
- **[GraalVM](https://www.graalvm.org)** - High-performance JDK with native image support

## 📢 Stay informed about stable releases

[![GitHub releases](https://img.shields.io/github/v/release/SoftInstigate/facet?include_prereleases&label=latest%20release)](https://github.com/SoftInstigate/facet/releases)

Facet is currently in active development and APIs may change.

If you want to be notified as soon as the project reaches a **stable release**:

- Click **Watch → Custom → Releases** at the top of this repository  
- Or follow the Releases page directly:  
  👉 https://github.com/SoftInstigate/facet/releases

We will use GitHub Releases to announce:

- the first stable version (1.0.0),
- breaking changes,
- important new features and migration notes.

## Quick Start

1. Set up RESTHeart with your MongoDB database
2. Add Facet plugin to your RESTHeart configuration
3. Create a template at `templates/yourdb/yourcollection/index.html`
4. Access `GET /yourdb/yourcollection` with `Accept: text/html` to see server-rendered HTML

## Documentation

- **[Developer's Guide](docs/DEVELOPERS_GUIDE.md)** - Complete reference for Facet features and capabilities
- **[Template Context Reference](docs/TEMPLATE_CONTEXT_REFERENCE.md)** - All available template variables

## Examples

Examples coming soon.

## License

Apache License 2.0
