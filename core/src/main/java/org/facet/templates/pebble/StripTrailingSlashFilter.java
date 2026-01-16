package org.facet.templates.pebble;

import java.util.List;
import java.util.Map;

import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

/**
 * Pebble filter that removes trailing slashes from paths.
 *
 * Usage in templates:
 * {{ path | stripTrailingSlash }}
 *
 * Examples:
 * - "/api/testdb/" → "/api/testdb"
 * - "/api/testdb" → "/api/testdb"
 * - "/" → "/" (root path preserved)
 */
public class StripTrailingSlashFilter implements Filter {

    @Override
    public Object apply(Object input, Map<String, Object> args, PebbleTemplate self,
            EvaluationContext context, int lineNumber) {

        if (input == null) {
            return null;
        }

        final String path = input.toString();

        // Preserve root path "/"
        if (path.length() <= 1) {
            return path;
        }

        // Remove trailing slash if present
        if (path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }

        return path;
    }

    @Override
    public List<String> getArgumentNames() {
        return List.of(); // No arguments needed
    }
}
