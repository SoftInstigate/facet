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

- Path-based template resolution
- [HTMX](https://htmx.org) support for partial updates
- [Pebble templates](https://pebbletemplates.io) (Twig/Jinja syntax)
- Generic handlers for MongoDB and JSON responses
- Built on [RESTHeart Backend Framework](https//restheart.org) for production-ready APIs

## Quick Start

1. Set up RESTHeart with your MongoDB database
2. Add Facet plugin to your RESTHeart configuration
3. Create a template at `templates/yourdb/yourcollection/index.html`
4. Access `GET /yourdb/yourcollection` with `Accept: text/html` to see server-rendered HTML

## Documentation

See the [Developer's Guide](docs/DEVELOPERS_GUIDE.md)

## Examples

Examples coming soon.

## License

Apache License 2.0