package org.facet.integration;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.nio.file.Paths;
import java.time.Duration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Integration tests for JavaScript plugins (services and interceptors).
 *
 * <p>Requires Docker. The RESTHeart + Facet image is built by the test itself from the packaged
 * {@code target/facet-core.jar} and {@code target/lib}, mirroring the root {@code Dockerfile}; the
 * base image is {@code softinstigate/restheart:<restheart-commons.version>} (system property
 * {@code restheart.image}, set by the Failsafe plugin). Without Docker the test is skipped.
 *
 * <p>Run with: {@code mvn -pl core verify} (or {@code -Dit.test=JsPluginsIT})
 */
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("JavaScript Plugins Integration Tests")
class JsPluginsIT {

    private static final Network NETWORK = Network.newNetwork();

    private static final int RESTHEART_PORT = 8080;

    private static final String RESTHEART_IMAGE = System.getProperty("restheart.image",
            "softinstigate/restheart:9.12.3");

    // Same layering as the root Dockerfile, built from the module's own build output
    private static final ImageFromDockerfile FACET_IMAGE = new ImageFromDockerfile()
            .withFileFromPath("plugins/facet-core.jar", Paths.get("target/facet-core.jar"))
            .withFileFromPath("plugins/lib", Paths.get("target/lib"))
            .withDockerfileFromBuilder(b -> b
                    .from(RESTHEART_IMAGE)
                    .copy("plugins/facet-core.jar", "/opt/restheart/plugins/")
                    .copy("plugins/lib/", "/opt/restheart/plugins/")
                    .cmd("-o", "/opt/restheart/etc/restheart.yml")
                    .build());

    @SuppressWarnings({ "resource", "null" })
    @Container
    private static final GenericContainer<?> MONGO = new GenericContainer<>(DockerImageName.parse("mongo:8.0"))
            .withNetwork(NETWORK)
            .withNetworkAliases("mongodb")
            .withCommand("mongod", "--replSet", "rs0", "--bind_ip_all")
            .waitingFor(Wait.forLogMessage(".*Waiting for connections.*", 1)
                    .withStartupTimeout(Duration.ofSeconds(30)));

    @SuppressWarnings({ "resource", "null" })
    @Container
    private static final GenericContainer<?> RESTHEART = new GenericContainer<>(FACET_IMAGE)
                    .withNetwork(NETWORK)
                    .dependsOn(MONGO)
                    .withExposedPorts(RESTHEART_PORT)
                    .withEnv("RHO",
                            "/mclient/connection-string->\"mongodb://mongodb\";"
                                    + "/http-listener/host->\"0.0.0.0\";")
                    .withCommand("-o", "/opt/restheart/etc/restheart.yml")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(
                                    "../examples/product-catalog/plugins/product-stats"),
                            "/opt/restheart/plugins/product-stats/")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(
                                    "../examples/product-catalog/plugins/request-logger"),
                            "/opt/restheart/plugins/request-logger/")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(
                                    "../examples/product-catalog/templates"),
                            "/opt/restheart/templates/")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(
                                    "../examples/product-catalog/restheart.yml"),
                            "/opt/restheart/etc/restheart.yml")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(
                                    "../examples/product-catalog/users.yml"),
                            "/opt/restheart/etc/users.yml")
                    .waitingFor(Wait.forHttp("/ping").forPort(RESTHEART_PORT)
                            .withStartupTimeout(Duration.ofSeconds(60)));

    @BeforeAll
    static void setUp() {
        // Initialize MongoDB replica set
        try {
            MONGO.execInContainer("mongosh", "--quiet", "--eval",
                    "try { rs.initiate() } catch(e) {}");
            Thread.sleep(5000); // Wait for replica set election
        } catch (Exception _) {
            // Ignore
        }

        baseURI = "http://" + RESTHEART.getHost() + ":" + RESTHEART.getMappedPort(RESTHEART_PORT);
    }

    // ── Service: product-stats (JSON) ──────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("product-stats service returns 200 with JSON")
    void productServiceReturnsJson() {
        given()
                .auth().preemptive().basic("admin", "secret")
                .accept("application/json")
        .when()
                .get("/shop/stats")
        .then()
                .statusCode(200)
                .contentType("application/json")
                .body("total", notNullValue())
                .body("categories", notNullValue())
                .body("avgPrice", notNullValue());
    }

    @Test
    @Order(2)
    @DisplayName("product-stats JSON contains expected fields")
    void productServiceJsonHasExpectedFields() {
        given()
                .auth().preemptive().basic("admin", "secret")
                .accept("application/json")
        .when()
                .get("/shop/stats")
        .then()
                .statusCode(200)
                .body("inStock", notNullValue())
                .body("outOfStock", notNullValue())
                .body("totalValue", notNullValue())
                .body("minPrice", notNullValue())
                .body("maxPrice", notNullValue())
                .body("lowStock", notNullValue());
    }

    // ── Service: product-stats (HTML via Facet) ────────────────────────

    @Test
    @Order(3)
    @DisplayName("product-stats renders HTML via Facet template")
    void productServiceRendersHtml() {
        given()
                .auth().preemptive().basic("admin", "secret")
                .accept("text/html")
        .when()
                .get("/shop/stats")
        .then()
                .statusCode(200)
                .contentType(containsString("text/html"))
                .body(containsString("Inventory Stats"))
                .body(containsString("Total Products"));
    }

    @Test
    @Order(4)
    @DisplayName("HTML template uses direct variables (no 'document.' prefix)")
    void htmlTemplateUsesDirectVariables() {
        given()
                .auth().preemptive().basic("admin", "secret")
                .accept("text/html")
        .when()
                .get("/shop/stats")
        .then()
                .statusCode(200)
                .body(not(containsString("document.")));
    }

    // ── Interceptor: request-logger ────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("request-logger interceptor adds X-Facet-Plugin header")
    void interceptorAddsPluginHeader() {
        given()
                .auth().preemptive().basic("admin", "secret")
        .when()
                .get("/shop/stats")
        .then()
                .header("X-Facet-Plugin", "request-logger");
    }

    @Test
    @Order(6)
    @DisplayName("request-logger interceptor adds X-Request-Path header")
    void interceptorAddsRequestPathHeader() {
        given()
                .auth().preemptive().basic("admin", "secret")
        .when()
                .get("/shop/stats")
        .then()
                .header("X-Request-Path", "/shop/stats");
    }

    @Test
    @Order(7)
    @DisplayName("request-logger interceptor adds X-Request-Method header")
    void interceptorAddsMethodHeader() {
        given()
                .auth().preemptive().basic("admin", "secret")
        .when()
                .get("/shop/stats")
        .then()
                .header("X-Request-Method", "GET");
    }

    // ── Authentication ─────────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("unauthenticated request returns 401")
    void unauthenticatedReturns401() {
        when()
                .get("/shop/stats")
        .then()
                .statusCode(401);
    }
}
