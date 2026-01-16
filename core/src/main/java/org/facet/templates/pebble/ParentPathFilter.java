package org.facet.templates.pebble;

import java.util.List;
import java.util.Map;

import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

/**
 * Pebble filter that returns the parent path by removing the last segment.
 *
 * <p>Usage in templates:
 * <pre>
 * {{ path | parentPath }}
 * </pre>
 *
 * <p>Examples:
 * <ul>
 * <li>"/api/testdb/docs/item123" → "/api/testdb/docs"</li>
 * <li>"/testdb/docs" → "/testdb"</li>
 * <li>"/testdb" → "/"</li>
 * <li>"/" → "/"</li>
 * </ul>
 *
 * <p>This filter is mount-agnostic and works correctly with any MongoDB mount configuration.
 */
public class ParentPathFilter implements Filter {

    @Override
    public Object apply(Object input, Map<String, Object> args, PebbleTemplate self,
            EvaluationContext context, int lineNumber) {

        if (input == null) {
            return null;
        }

        String path = input.toString();

        // Handle empty or root path
        if (path.isEmpty() || path.equals("/")) {
            return "/";
        }

        // Remove trailing slash if present
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        // Find last slash
        final int lastSlash = path.lastIndexOf('/');

        // If no slash found or it's the first character, return root
        if (lastSlash <= 0) {
            return "/";
        }

        // Return path up to (but not including) the last slash
        return path.substring(0, lastSlash);
    }

    @Override
    public List<String> getArgumentNames() {
        return List.of(); // No arguments needed
    }
}
