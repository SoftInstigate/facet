# Dockerfile for Facet Examples
# Builds a RESTHeart image with Facet plugin pre-installed
#
# Build context should be the repository root:
#   docker build -f examples/Dockerfile -t facet:latest .

FROM softinstigate/restheart:9

# Copy Facet core plugin into RESTHeart plugins directory
COPY core/target/facet-core.jar /opt/restheart/plugins/facet-core.jar

# Copy default Facet configuration (development-friendly)
# This can be overridden by mounting a custom config at /opt/restheart/etc/restheart.yml
COPY examples/shared/restheart-base.yml /opt/restheart/etc/restheart.yml

# The base image entrypoint starts RESTHeart with the configuration at /opt/restheart/etc/restheart.yml
# Plugin is loaded automatically from /opt/restheart/plugins/
# Examples can override config by mounting their own restheart.yml
#
# Note: RESTHeart uses a distroless base image (no shell), so we cannot use RUN commands
# to verify files. The COPY commands will fail at build time if files don't exist.
