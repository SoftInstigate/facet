package org.facet.templates.pebble;

import java.util.Map;

import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Filter;

/**
 * Custom Pebble extension that registers additional filters for template processing.
 */
public class CustomPebbleExtension extends AbstractExtension {

    @Override
    public Map<String, Filter> getFilters() {
        return Map.of(
            "stripTrailingSlash", new StripTrailingSlashFilter(),
            "buildPath", new BuildPathFilter(),
            "parentPath", new ParentPathFilter(),
            "toJson", new ToJsonFilter()
        );
    }
}
