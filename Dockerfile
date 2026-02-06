# Facet base image (RESTHeart + Facet plugin)

FROM softinstigate/restheart:9

# Copy locally built plugin artifacts
COPY core/target/facet-core.jar /opt/restheart/plugins/
COPY core/target/lib/*.jar /opt/restheart/plugins/

# Use explicit config file location (mounted by docker-compose)
CMD ["-o", "/opt/restheart/etc/restheart.yml"]
