package org.facet.html.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.json.JsonWriterSettings;
import org.facet.html.internal.IdTypeDetector;
import org.facet.templates.TemplateContextBuilder;
import org.facet.templates.TemplateProcessor;
import org.restheart.exchange.ExchangeKeys;
import org.restheart.exchange.MongoRequest;
import org.restheart.exchange.MongoResponse;
import org.restheart.exchange.ServiceRequest;
import org.restheart.exchange.ServiceResponse;
import org.restheart.mongodb.utils.MongoMountResolver;
import org.restheart.mongodb.utils.MongoMountResolver.ResolvedContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoClient;

/**
 * Handler for MongoDB BSON responses with pagination support.
 *
 * <p>
 * This handler processes MongoDB responses and builds template contexts with:
 * <ul>
 * <li>BSON document transformation to JSON</li>
 * <li>Pagination (page, pagesize, totalPages, totalDocuments)</li>
 * <li>MongoDB metadata (database, collection)</li>
 * <li>Mount resolution (mongo-mounts configuration)</li>
 * <li>Query parameters (filter, keys, sort)</li>
 * </ul>
 *
 * @see HtmlResponseHandler
 * @see MongoHtmlResponseInterceptor
 */
public class MongoHtmlResponseHandler implements HtmlResponseHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoHtmlResponseHandler.class);

    private final MongoClient mongoClient;
    private final TemplateProcessor templateProcessor;

    /**
     * Creates a new MongoHtmlResponseHandler.
     *
     * @param mongoClient the MongoDB client for counting documents
     * @param templateProcessor the template processor for global context
     */
    public MongoHtmlResponseHandler(
            final MongoClient mongoClient,
            final TemplateProcessor templateProcessor) {
        this.mongoClient = mongoClient;
        this.templateProcessor = templateProcessor;

        // Log Facet core build timestamp on first instantiation
        logFacetCoreVersion();
    }

    /**
     * Logs Facet core version information including build timestamp.
     */
    private static void logFacetCoreVersion() {
        LOGGER.info("Facet core MongoHtmlResponseHandler loaded with BSON unwrapping support");

        try {
            final var classFile = MongoHtmlResponseHandler.class.getResource(
                MongoHtmlResponseHandler.class.getSimpleName() + ".class");

            if (classFile != null) {
                final var connection = classFile.openConnection();
                final var lastModified = connection.getLastModified();
                if (lastModified > 0) {
                    final var buildTime = new java.util.Date(lastModified);
                    LOGGER.info("Facet core build timestamp: {}", buildTime);
                } else {
                    LOGGER.info("Facet core build timestamp: unable to determine (lastModified=0)");
                }
            } else {
                LOGGER.info("Facet core build timestamp: unable to determine (classFile=null)");
            }
        } catch (Exception e) {
            LOGGER.warn("Could not determine Facet core build timestamp", e);
        }
    }

    /**
     * Checks if this is a MongoDB request/response pair.
     * Handles all MongoDB responses including database listings, collection listings,
     * and document listings.
     */
    @Override
    public boolean canHandle(final ServiceRequest<?> request, final ServiceResponse<?> response) {
        return request instanceof MongoRequest && response instanceof MongoResponse;
    }

    /**
     * Builds template context with MongoDB-specific data and pagination.
     */
    @Override
    public Map<String, Object> buildContext(final ServiceRequest<?> request, final ServiceResponse<?> response) {

        // Cast to MongoDB types
        final MongoRequest mongoRequest = (MongoRequest) request;
        final MongoResponse mongoResponse = (MongoResponse) response;
        final BsonValue responseContent = mongoResponse.getContent();

        LOGGER.debug("Building MongoDB template context for path: {}", request.getPath());

        // Get resolved context from request (lazy-initialized by MongoService)
        final ResolvedContext mountContext = mongoRequest.getResolvedContext();

        // Extract tenant ID using static utility method
        final var tenantId = MongoMountResolver.extractTenantId(mongoRequest);

        // Tenant-aware filtering ONLY for database listings (when db=null, coll=null)
        // For parametric mounts like /{host[0]}/{*}, root shows collections (db!=null), not databases
        final BsonValue tenantScopedContent = isMongoRequestForDatabaseListing(mongoRequest)
            ? filterDatabasesByTenant(responseContent, tenantId)
            : responseContent;

        // Process documents and add ID type information
        // This enriched list contains both full document data AND ID type metadata
        final var documents = enrichDocumentsWithIdType(tenantScopedContent);

        // Start building context
        final var builder = createTemplateContextBuilder(mongoRequest, tenantScopedContent, documents);

        // Add pagination context
        addPaginationContext(builder, mongoRequest, tenantScopedContent);

        // Add mount-aware context
        if (mountContext != null) {
            resolveMountContext(mongoRequest, mountContext, builder);
        } else {
            LOGGER.warn("ResolvedContext is null - permission flags will not be set");
        }

        return builder.build();
    }

    private boolean isMongoRequestForDatabaseListing(final MongoRequest mongoRequest) {
        return mongoRequest.getDBName() == null && mongoRequest.getCollectionName() == null;
    }

    /**
     * Creates a template context builder with MongoDB-specific variables.
     *
     * <p>
     * Template context variables provided:
     * <ul>
     * <li><b>path</b> (String): Full request path as received (e.g., "/api/testdb/mycoll")
     * <br>
     * Use this for building navigation links.</li>
     *
     * <li><b>mongoPath</b> (String): Clean MongoDB resource path for navigation
     * <br>
     * This is the path with MongoDB mount prefix stripped and trailing slashes removed.
     * <br>
     * Example: "/api/testdb/mycoll/" → "/testdb/mycoll"
     * <br>
     * Use this for breadcrumb segments and displaying the resource path.</li>
     *
     * <li><b>items</b> (List&lt;Map&gt;): List of enriched items (databases, collections, or documents)
     * <br>
     * Content varies by resourceType: database names, collection names, or documents with ID metadata.</li>
     *
     * <li><b>resourceType</b> (String): MongoDB resource type (ROOT, DATABASE, COLLECTION, DOCUMENT)</li>
     *
     * <li><b>data</b> (String): Raw JSON representation of the MongoDB response</li>
     *
     * <li><b>filter, keys, sort</b> (String): Query parameters for MongoDB operations</li>
     * </ul>
     *
     * <p>
     * Global context variables (set by BrowserService):
     * <ul>
     * <li><b>mongoPrefix</b> (String): MongoDB mount prefix (e.g., "/api" or "/")
     * <br>
     * Available globally, set by BrowserService from MongoMountResolver.</li>
     * </ul>
     */
    private TemplateContextBuilder createTemplateContextBuilder(
            final MongoRequest mongoRequest,
            final BsonValue tenantScopedContent,
            final List<Map<String, Object>> documents) {

        final String path = mongoRequest.getPath();
        final String mongoPrefix = getMongoPrefix();
        final String mongoPath = normalizeTrailingSlash(stripMongoPrefix(path, mongoPrefix));

        final var builder = TemplateContextBuilder.create(templateProcessor.getGlobalTemplateContext())
                .withAuthenticatedUser(mongoRequest)
                .with("path", path)
                .with("mongoPath", mongoPath)
                .with("items", documents)
                .with("resourceType", mongoRequest.getType())
                .with("data", transformBsonToJsonList(tenantScopedContent))
                .with("filter", mongoRequest.getQueryParameterOrDefault("filter", ""))
                .with("keys", mongoRequest.getQueryParameterOrDefault("keys", ""))
                .with("sort", mongoRequest.getQueryParameterOrDefault("sort", ""));

        // Add tenant-aware context for parametric mounts
        addTenantContext(builder, mongoRequest);

        return builder;
    }

    /**
     * Adds tenant-aware context derived from hostname-based parametric mounts.
     * Populates tenantId, isMultiTenant, and hostParams.
     */
    private void addTenantContext(final TemplateContextBuilder builder, final MongoRequest mongoRequest) {
        final Optional<String> tenantId = MongoMountResolver.extractTenantId(mongoRequest);
        final ResolvedContext mountContext = mongoRequest.getResolvedContext();
        final boolean isMultiTenant = mountContext != null
                && mountContext.hasParametricMounts()
                && tenantId.isPresent();

        builder.with("tenantId", tenantId.map(id -> "\"" + id + "\"").orElse("null"));
        builder.with("isMultiTenant", isMultiTenant);
        builder.with("hostParams", buildHostnameParamsAsJson(mongoRequest));
    }

    /**
     * Gets the MongoDB mount prefix from global template context (set by BrowserService).
     * 
     * @return MongoDB mount prefix (e.g., "/api") or "/" if not set
     */
    private String getMongoPrefix() {
        if (templateProcessor == null) {
            return "/";
        }
        final Object prefix = templateProcessor.getGlobalTemplateContext().get("mongoPrefix");
        return prefix != null
            ? prefix.toString()
            : "/";
    }

    /**
     * Strips the MongoDB mount prefix from a path.
     * 
     * @param path The full request path (e.g., "/api/testdb/mycoll")
     * @param mongoPrefix The MongoDB mount prefix (e.g., "/api")
     * @return Path without prefix (e.g., "/testdb/mycoll")
     */
    private String stripMongoPrefix(final String path, final String mongoPrefix) {
        if (mongoPrefix == null || mongoPrefix.equals("/") || path == null) {
            return path;
        }
        if (path.startsWith(mongoPrefix)) {
            final String stripped = path.substring(mongoPrefix.length());
            return stripped.isEmpty()
                ? "/"
                : stripped;
        }
        return path;
    }

    /**
     * Normalizes a path by removing trailing slashes (except for root "/").
     * 
     * @param path The path to normalize (e.g., "/api/" or "/api/testdb/")
     * @return Normalized path (e.g., "/api" or "/api/testdb")
     */
    private String normalizeTrailingSlash(final String path) {
        if (path == null || path.length() <= 1) {
            return path;
        }
        return path.endsWith("/")
            ? path.substring(0, path.length() - 1)
            : path;
    }

    /**
     * Builds hostname parameters as JSON string for direct template injection.
     */
    private String buildHostnameParamsAsJson(final MongoRequest request) {
        final Map<String, String> params = new HashMap<>();

        if (request == null) {
            return "{}";
        }

        final String hostHeader = request.getHeader("Host");
        if (hostHeader == null || hostHeader.isEmpty()) {
            return "{}";
        }

        final String hostWithoutPort = hostHeader.contains(":")
            ? hostHeader.substring(0, hostHeader.indexOf(":"))
            : hostHeader;

        params.put("host", hostWithoutPort);

        final String[] parts = hostWithoutPort.split("\\.");
        for (int i = 0; i < parts.length; i++) {
            params.put("host[" + i + "]", parts[i]);
        }

        return buildJsonFromParams(params);
    }

    private String buildJsonFromParams(final Map<String, String> params) {
        final StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (final var entry : params.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
            first = false;
        }
        json.append("}");
        return json.toString();
    }

    private void resolveMountContext(
            final MongoRequest mongoRequest,
            final ResolvedContext mountContext,
            final TemplateContextBuilder builder) {
        LOGGER.debug("Resolving mount context for path: {}, dbName: {}, collName: {}, pathParams: {}",
                mongoRequest.getPath(),
                mongoRequest.getDBName(),
                mongoRequest.getCollectionName(),
                mongoRequest.getPathTemplateParameters());

        LOGGER.debug("Mount context resolved: db={}, coll={}, canCreateColls={}",
                mountContext.database(),
                mountContext.collection(),
                mountContext.canCreateCollections());

        // Resolve database and collection (prefer mount context if available)
        final String resolvedDb = mountContext.database() != null
            ? mountContext.database()
            : mongoRequest.getDBName();
        final String resolvedColl = mountContext.collection() != null
            ? mountContext.collection()
            : mongoRequest.getCollectionName();

        builder
                .with("db", resolvedDb)
                .with("coll", resolvedColl)
                .with("canCreateDatabases", mountContext.canCreateDatabases())
                .with("canCreateCollections", mountContext.canCreateCollections())
                .with("canCreateDocuments", mongoRequest.getType() != ExchangeKeys.TYPE.DOCUMENT
                        && mongoRequest.getCollectionName() != null)
                .with("canDeleteDatabase", mountContext.canDeleteDatabase())
                .with("canDeleteCollection", mountContext.canDeleteCollection())
                .with("resourceUrl", mountContext.mongoResourcePath());

        // Derive a collection-level reset path (avoids landing on document path with page params)
        final String collectionUrl = getResetPath(mongoRequest, mountContext);
        builder.with("collectionUrl", collectionUrl != null
            ? collectionUrl
            : mongoRequest.getPath());
    }

    private String getResetPath(final MongoRequest mongoRequest, final ResolvedContext mountContext) {
        String resetPath = mountContext.mongoResourcePath();
        if (resetPath != null && mongoRequest.getCollectionName() != null
                && mongoRequest.getType() == ExchangeKeys.TYPE.DOCUMENT) {
            final int lastSlash = resetPath.lastIndexOf('/');
            if (lastSlash > 0) {
                resetPath = resetPath.substring(0, lastSlash);
            }
        }
        return resetPath;
    }

    /**
     * Adds pagination context to the template context builder.
     */
    private void addPaginationContext(
            final TemplateContextBuilder builder,
            final MongoRequest request,
            final BsonValue responseContent) {

        final long totalItems = calculateTotalDocuments(request, responseContent);
        final int pagesize = request.getPagesize();
        final int totalPages = calculateTotalPages(totalItems, pagesize);

        builder
                .with("page", request.getPage())
                .with("pagesize", pagesize)
                .with("totalItems", totalItems)
                .with("totalPages", totalPages);

        LOGGER.debug("Pagination: page {} of {} ({} total items)",
                request.getPage(), totalPages, totalItems);
    }

    private long calculateTotalDocuments(final MongoRequest request, final BsonValue responseContent) {
        // Determine what we're listing and count accordingly
        final String dbName = request.getDBName();
        final String collectionName = request.getCollectionName();
        final boolean responseContentIsArray = responseContent.isArray();

        if (dbName != null && collectionName != null && responseContentIsArray) {
            // Listing documents in a collection
            final var filter = request.getFiltersDocument();
            if (filter != null && !filter.isEmpty()) {
                return countDocumentsWithFilter(request, filter);
            } else {
                return estimatedDocumentCount(request);
            }
        }

        if (dbName != null && collectionName == null && responseContentIsArray) {
            // Listing collections in a database
            return countCollections(dbName);
        }

        if (isMongoRequestForDatabaseListing(request) && responseContentIsArray) {
            // Listing databases
            return countDatabases();
        }

        // Single document or unknown type - total documents is 1
        return 1;
    }

    /**
     * Estimates the total number of documents in the collection.
     */
    private long estimatedDocumentCount(final MongoRequest request) {
        return mongoClient.getDatabase(request.getDBName())
                .getCollection(request.getCollectionName())
                .estimatedDocumentCount();
    }

    /**
     * Counts documents matching the filter.
     */
    private long countDocumentsWithFilter(final MongoRequest request, final BsonDocument filter) {
        return mongoClient.getDatabase(request.getDBName())
                .getCollection(request.getCollectionName())
                .countDocuments(filter);
    }

    /**
     * Calculates total pages based on document count and page size.
     */
    private static int calculateTotalPages(final long totalDocuments, final int pagesize) {
        return Math.max(1, (int) Math.ceil((double) totalDocuments / pagesize));
    }

    /**
     * Filters database listings to the tenant's database plus system databases when isolated.
     */
    public static BsonValue filterDatabasesByTenant(final BsonValue responseContent, final Optional<String> tenantId) {
        if (tenantId.isEmpty() || responseContent == null || !responseContent.isArray()) {
            return responseContent;
        }

        final BsonArray filtered = new BsonArray();
        responseContent.asArray().forEach(value -> {
            if (value.isString()) {
                final String name = value.asString().getValue();
                if (isSystemDatabase(name) || name.equals(tenantId.get())) {
                    filtered.add(value);
                }
            } else {
                filtered.add(value);
            }
        });

        return filtered;
    }

    /**
     * Counts the total number of databases, excluding system databases.
     * RESTHeart filters out system databases (admin, config, local) from listings.
     */
    private long countDatabases() {
        return mongoClient.listDatabaseNames()
                .into(new ArrayList<>())
                .stream()
                .filter(name -> !isSystemDatabase(name))
                .count();
    }

    /**
     * Checks if a database is a system database that should be filtered out.
     */
    private static boolean isSystemDatabase(final String dbName) {
        return "admin".equals(dbName) || "config".equals(dbName) || "local".equals(dbName);
    }

    /**
     * Counts the total number of collections in the specified database, excluding system collections.
     * RESTHeart filters out system collections (those starting with "system.") from listings.
     */
    private long countCollections(final String databaseName) {
        if (databaseName == null || databaseName.isEmpty()) {
            return 0;
        }
        return mongoClient.getDatabase(databaseName)
                .listCollectionNames()
                .into(new ArrayList<>())
                .stream()
                .filter(name -> !isSystemCollection(name))
                .count();
    }

    /**
     * Checks if a collection is a system collection that should be filtered out.
     * RESTHeart filters out collections starting with "system." or "_" (like _properties).
     */
    private boolean isSystemCollection(final String collectionName) {
        return collectionName.startsWith("system.") || collectionName.startsWith("_");
    }

    /**
     * Transforms BSON data to formatted JSON strings with ID type metadata.
     * Adds __id_type__ field to documents for JavaScript to use when making API calls.
     * This ensures a single source of truth for ID type detection.
     */
    private static List<String> transformBsonToJsonList(final BsonValue bsonData) {
        final var documents = new ArrayList<String>();
        final var settings = JsonWriterSettings.builder().indent(true).build();

        if (bsonData.isDocument()) {
            documents.add(enrichJsonWithIdType(bsonData.asDocument(), settings));
        } else if (bsonData.isArray()) {
            final var bsonArray = bsonData.asArray();
            for (final var value : bsonArray) {
                switch (value) {
                    case final BsonDocument doc -> documents.add(enrichJsonWithIdType(doc, settings));
                    case final BsonString str -> documents.add("\"" + str.getValue() + "\"");
                    default -> documents.add(value.toString());
                }
            }
        }

        return documents;
    }

    /**
     * Enriches a BSON document's JSON representation with ID type metadata.
     * Adds __id_type__ field that JavaScript can use to construct correct URLs.
     *
     * @param doc the BSON document
     * @param settings JSON writer settings for formatting
     * @return JSON string with ID type metadata
     */
    private static String enrichJsonWithIdType(final BsonDocument doc, final JsonWriterSettings settings) {
        if (!doc.containsKey("_id")) {
            // No _id field, return as-is
            return doc.toJson(settings);
        }

        final String idType = IdTypeDetector.detectIdType(doc.get("_id"));

        // Clone the document and add ID type metadata
        final BsonDocument enrichedDoc = doc.clone();

        // Only add __id_type__ if it's not an ObjectId (which is the default)
        if (idType != null) {
            enrichedDoc.put("__id_type__", new org.bson.BsonString(idType));
        }

        return enrichedDoc.toJson(settings);
    }

    /**
     * Enriches documents with ID type information for navigation and display.
     * Converts BSON documents to Maps containing:
     * - Full BSON document (for display/pagination)
     * - ID type metadata (for generating correct URLs)
     *
     * @param responseContent the BSON response content (array or document)
     * @return list of maps with full document data + ID type info
     */
    private static List<Map<String, Object>> enrichDocumentsWithIdType(final BsonValue responseContent) {
        final var enrichedDocs = new ArrayList<Map<String, Object>>();

        if (responseContent == null) {
            return enrichedDocs;
        }

        if (responseContent.isArray()) {
            enrichBsonArray(responseContent.asArray(), enrichedDocs);
        } else if (responseContent.isDocument()) {
            enrichedDocs.add(enrichSingleDocument(responseContent.asDocument()));
        }

        return enrichedDocs;
    }

    private static void enrichBsonArray(
            final BsonArray bsonArray,
            final List<Map<String, Object>> enrichedDocs) {

        for (final var arrayElement : bsonArray) {
            switch (arrayElement) {
                case final BsonDocument doc -> enrichedDocs.add(enrichSingleDocument(doc));
                case final BsonString str -> enrichedDocs.add(getEnrichedDoc(str.getValue()));
                default -> enrichedDocs.add(getEnrichedDoc(arrayElement.toString()));
            }
        }
    }

    private static Map<String, Object> getEnrichedDoc(final String value) {
        final var enrichedDoc = new HashMap<String, Object>();
        enrichedDoc.put("value", value);
        enrichedDoc.put("isString", true);
        return enrichedDoc;
    }

    private static Map<String, Object> enrichSingleDocument(final BsonDocument documentBsonValue) {
        final var enrichedDoc = new HashMap<String, Object>();

        enrichedDoc.put("data", bsonDocumentToMap(documentBsonValue));
        enrichedDoc.put("isString", false);

        if (documentBsonValue.containsKey("_id")) {
            final var idValue = documentBsonValue.get("_id");
            final var idType = IdTypeDetector.detectIdType(idValue);

            // Add _id metadata for navigation
            final var idInfo = new HashMap<String, Object>();
            idInfo.put("value", IdTypeDetector.extractIdValue(idValue));
            idInfo.put("type", idType);
            idInfo.put("needsParam", idType != null);

            enrichedDoc.put("_id", idInfo);
        }

        return enrichedDoc;
    }

    /**
     * Converts a BSON document to a plain Java Map, unwrapping all BSON values.
     * This allows templates to access values directly without BSON type wrappers.
     *
     * @param doc the BSON document to convert
     * @return a Map with unwrapped Java values
     */
    private static Map<String, Object> bsonDocumentToMap(final BsonDocument doc) {
        final var map = new HashMap<String, Object>();
        for (final var entry : doc.entrySet()) {
            map.put(entry.getKey(), unwrapBsonValue(entry.getValue()));
        }
        return map;
    }

    /**
     * Unwraps a BSON value to its plain Java equivalent.
     *
     * @param value the BSON value to unwrap
     * @return the unwrapped Java value
     */
    private static Object unwrapBsonValue(final BsonValue value) {
        if (value == null || value.isNull()) {
            return null;
        }

        return switch (value.getBsonType()) {
            case STRING -> value.asString().getValue();
            case INT32 -> value.asInt32().getValue();
            case INT64 -> value.asInt64().getValue();
            case DOUBLE -> value.asDouble().getValue();
            case BOOLEAN -> value.asBoolean().getValue();
            case OBJECT_ID -> value.asObjectId().getValue().toHexString();
            case DATE_TIME -> value.asDateTime().getValue();
            case TIMESTAMP -> value.asTimestamp().getValue();
            case DECIMAL128 -> value.asDecimal128().getValue().bigDecimalValue();
            case ARRAY -> {
                final var list = new ArrayList<>();
                for (final var item : value.asArray()) {
                    list.add(unwrapBsonValue(item));
                }
                yield list;
            }
            case DOCUMENT -> bsonDocumentToMap(value.asDocument());
            case BINARY -> value.asBinary().getData();
            case REGULAR_EXPRESSION -> value.asRegularExpression().getPattern();
            case JAVASCRIPT -> value.asJavaScript().getCode();
            case JAVASCRIPT_WITH_SCOPE -> value.asJavaScriptWithScope().getCode();
            case SYMBOL -> value.asSymbol().getSymbol();
            default -> value.toString(); // Fallback for unsupported types
        };
    }
}
