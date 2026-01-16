package org.facet.templates.pebble;

import java.util.List;
import java.util.Map;

import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

/**
 * Pebble filter that builds a path by appending a segment to a base path.
 *
 * Usage in templates:
 * {{ basePath | buildPath(segment) }}
 *
 * Examples:
 * - "/api/testdb" | buildPath("mycoll") → "/api/testdb/mycoll"
 * - "/" | buildPath("testdb") → "/testdb"
 * - "" | buildPath("testdb") → "/testdb"
 *
 * Handles edge cases:
 * - Empty base path → "/segment"
 * - Root base path "/" → "/segment"
 * - Normal path → "basePath/segment"
 */
public class BuildPathFilter implements Filter {

    @Override
    public Object apply(Object input, Map<String, Object> args, PebbleTemplate self,
            EvaluationContext context, int lineNumber) {

        if (input == null) {
            input = "";
        }

        final String basePath = input.toString();
        // Try positional argument first (Pebble syntax: path | buildPath(id))
        // Then try named argument (Pebble syntax: path | buildPath(segment=id))
        Object segmentObj = args.get("0");
        if (segmentObj == null) {
            segmentObj = args.get("segment");
        }
        final String segment = segmentObj != null ? segmentObj.toString() : "";

        // Handle empty base path or root path
        if (basePath.isEmpty() || basePath.equals("/")) {
            return "/" + segment;
        }

        // Normal case: append segment to base path
        return basePath + "/" + segment;
    }

    @Override
    public List<String> getArgumentNames() {
        return List.of("segment");
    }
}
