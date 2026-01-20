package org.facet.templates.pebble;

import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

/**
 * Pebble filter that serializes objects to pretty JSON strings.
 *
 * Usage in templates:
 * {{ doc.data | toJson }}
 * {{ doc.data | toJson(pretty=false) }}
 *
 * Examples:
 * - Map → JSON string
 * - List → JSON array string
 * - Supports pretty printing with indent
 */
public class ToJsonFilter implements Filter {

    private static final Gson COMPACT_GSON = new Gson();
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public String apply(
            final Object input,
            final Map<String, Object> args,
            final PebbleTemplate self,
            final EvaluationContext context,
            final int lineNumber) {

        if (input == null) {
            return "null";
        }

        // Check for pretty printing argument
        final var pretty = args.getOrDefault("pretty", true);

        final boolean usePretty = pretty instanceof Boolean pBoolean
            ? pBoolean.booleanValue()
            : Boolean.parseBoolean(pretty.toString());

        final var gson = usePretty
            ? PRETTY_GSON
            : COMPACT_GSON;

        return gson.toJson(input);
    }

    @Override
    public List<String> getArgumentNames() {
        return List.of("pretty"); // Optional argument for formatting
    }
}
