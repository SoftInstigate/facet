package org.facet.html.internal;

import org.bson.BsonValue;

/**
 * Utility class for detecting MongoDB document ID types and generating RESTHeart id_type query parameters.
 *
 * <p>
 * RESTHeart requires an id_type query parameter for document URLs when the _id is not an ObjectId.
 * This class maps BSON types to RESTHeart's id_type values.
 *
 * @see <a href="https://restheart.org/docs/mongodb-rest/resource-uri#document-id-types">RESTHeart Document ID Types</a>
 */
public class IdTypeDetector {

    /**
     * RESTHeart id_type query parameter values.
     */
    public static final String ID_TYPE_OID = "OID";
    public static final String ID_TYPE_STRING = "STRING";
    public static final String ID_TYPE_NUMBER = "NUMBER";
    public static final String ID_TYPE_BOOLEAN = "BOOLEAN";
    public static final String ID_TYPE_DATE = "DATE";
    public static final String ID_TYPE_NULL = "NULL";
    public static final String ID_TYPE_MINKEY = "MINKEY";
    public static final String ID_TYPE_MAXKEY = "MAXKEY";

    /**
     * Detects the RESTHeart id_type for a given BSON value.
     *
     * @param bsonValue the BSON value (typically from document._id)
     * @return the RESTHeart id_type string, or null if type is ObjectId (default, no parameter needed)
     */
    public static String detectIdType(final BsonValue bsonValue) {
        if (bsonValue == null || bsonValue.isNull()) {
            return ID_TYPE_NULL;
        }

        return switch (bsonValue.getBsonType()) {
            case OBJECT_ID -> null; // ObjectId is the default - no id_type parameter needed
            case STRING -> ID_TYPE_STRING;
            case INT32, INT64, DOUBLE, DECIMAL128 -> ID_TYPE_NUMBER;
            case BOOLEAN -> ID_TYPE_BOOLEAN;
            case DATE_TIME -> ID_TYPE_DATE;
            case NULL -> ID_TYPE_NULL;
            case MIN_KEY -> ID_TYPE_MINKEY;
            case MAX_KEY -> ID_TYPE_MAXKEY;
            default -> ID_TYPE_STRING; // For other types (BINARY, ARRAY, DOCUMENT, etc.), treat as STRING
        };
    }

    /**
     * Builds a document URL with the correct id_type query parameter if needed.
     *
     * @param basePath the base path (e.g., "/mydb/mycollection")
     * @param documentId the document _id as a BSON value
     * @param queryParams existing query parameters (e.g., "page=1&pagesize=100")
     * @return the complete URL with id_type parameter if needed
     */
    public static String buildDocumentUrl(final String basePath, final BsonValue documentId, final String queryParams) {
        // Build base URL
        String url = basePath;
        if (!url.endsWith("/")) {
            url += "/";
        }
        url += extractIdValue(documentId);

        // Add query parameters
        final String idType = detectIdType(documentId);
        if (queryParams != null && !queryParams.isEmpty()) {
            url += "?" + queryParams;
            if (idType != null) {
                url += "&id_type=" + idType;
            }
        } else if (idType != null) {
            url += "?id_type=" + idType;
        }

        return url;
    }

    /**
     * Extracts the string representation of a document ID from a BSON value.
     *
     * @param bsonValue the BSON value (typically from document._id)
     * @return the string representation of the ID
     */
    public static String extractIdValue(final BsonValue bsonValue) {
        if (bsonValue == null || bsonValue.isNull()) {
            return "null";
        }

        return switch (bsonValue.getBsonType()) {
            case OBJECT_ID -> bsonValue.asObjectId().getValue().toHexString();
            case STRING -> bsonValue.asString().getValue();
            case INT32 -> String.valueOf(bsonValue.asInt32().getValue());
            case INT64 -> String.valueOf(bsonValue.asInt64().getValue());
            case DOUBLE -> String.valueOf(bsonValue.asDouble().getValue());
            case DECIMAL128 -> bsonValue.asDecimal128().getValue().toString();
            case BOOLEAN -> String.valueOf(bsonValue.asBoolean().getValue());
            case DATE_TIME -> String.valueOf(bsonValue.asDateTime().getValue());
            case NULL -> "null";
            case MIN_KEY, MAX_KEY -> "1";
            default -> bsonValue.toString(); // Fallback: use toString()
        };
    }

    private IdTypeDetector() {
        // Prevent instantiation
    }

}
