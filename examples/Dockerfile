# Dockerfile for Facet Examples
# Builds a RESTHeart image with Facet plugin pre-installed
#
# Build context should be the repository root:
#   docker build -f examples/Dockerfile -t facet:latest .

FROM softinstigate/restheart:9

# Copy Facet core plugin and its dependencies into RESTHeart plugins directory
COPY core/target/facet-core.jar /opt/restheart/plugins/facet-core.jar
COPY core/target/lib/*.jar /opt/restheart/plugins/

# Copy default Facet configuration (development-friendly)
COPY examples/shared/restheart-base.yml /opt/restheart/etc/restheart.yml

# Override CMD to explicitly specify config file location
# RESTHeart will load configuration from this path
CMD ["-o", "/opt/restheart/etc/restheart.yml"]

# Note: RESTHeart uses a distroless base image (no shell), so we cannot use RUN commands
# to verify files. The COPY commands will fail at build time if files don't exist.
