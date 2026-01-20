# Dockerfile for Facet Product Catalog Example
# Builds a RESTHeart image with Facet plugin pre-installed

FROM softinstigate/restheart:9

# Copy Facet core plugin and its dependencies into RESTHeart plugins directory
# References ../../core/target from product-catalog context
COPY ../../core/target/facet-core.jar /opt/restheart/plugins/facet-core.jar
COPY ../../core/target/lib/*.jar /opt/restheart/plugins/

# Note: Configuration is mounted via docker-compose.yml volumes
# This allows hot-reload during development without rebuilding the image

# Override CMD to explicitly specify config file location
CMD ["-o", "/opt/restheart/etc/restheart.yml"]

# Note: RESTHeart uses a distroless base image (no shell), so we cannot use RUN commands
# to verify files. The COPY commands will fail at build time if files don't exist.
