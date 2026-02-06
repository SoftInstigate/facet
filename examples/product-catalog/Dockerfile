# Dockerfile for Facet Product Catalog Example
# Uses the Facet image (RESTHeart + Facet plugin pre-installed)

FROM softinstigate/facet:latest

# Note: Configuration is mounted via docker-compose.yml volumes
# This allows hot-reload during development without rebuilding the image

# Override CMD to explicitly specify config file location
CMD ["-o", "/opt/restheart/etc/restheart.yml"]

# Note: RESTHeart uses a distroless base image (no shell), so we cannot use RUN commands.
