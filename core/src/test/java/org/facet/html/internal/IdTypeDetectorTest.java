package org.facet.html.internal;

import org.bson.BsonInt32;
import org.bson.BsonMaxKey;
import org.bson.BsonMinKey;
import org.bson.BsonObjectId;
import org.bson.BsonString;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link IdTypeDetector}.
 */
class IdTypeDetectorTest {

    @Test
    @DisplayName("ObjectId needs no id_type parameter")
    void objectIdHasNoIdType() {
        ObjectId oid = new ObjectId();
        assertNull(IdTypeDetector.detectIdType(new BsonObjectId(oid)));
        assertEquals("/db/coll/" + oid.toHexString(),
                IdTypeDetector.buildDocumentUrl("/db/coll", new BsonObjectId(oid), null));
    }

    @Test
    @DisplayName("String and number ids carry their id_type, after existing query parameters")
    void stringAndNumberIds() {
        assertEquals("/db/coll/abc?id_type=STRING",
                IdTypeDetector.buildDocumentUrl("/db/coll/", new BsonString("abc"), ""));
        assertEquals("/db/coll/42?page=2&id_type=NUMBER",
                IdTypeDetector.buildDocumentUrl("/db/coll", new BsonInt32(42), "page=2"));
    }

    @Test
    @DisplayName("MinKey/MaxKey use a placeholder path value: RESTHeart ignores it and only reads id_type")
    void minMaxKeyPlaceholder() {
        assertEquals("/db/coll/1?id_type=MINKEY",
                IdTypeDetector.buildDocumentUrl("/db/coll", new BsonMinKey(), null));
        assertEquals("/db/coll/1?id_type=MAXKEY",
                IdTypeDetector.buildDocumentUrl("/db/coll", new BsonMaxKey(), null));
    }

    @Test
    @DisplayName("Null id maps to id_type NULL")
    void nullId() {
        assertEquals("NULL", IdTypeDetector.detectIdType(null));
        assertEquals("null", IdTypeDetector.extractIdValue(null));
    }
}
