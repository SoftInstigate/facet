package org.facet.templates.pebble;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.facet.templates.TemplateProcessingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PebbleTemplateProcessor} using the file loader.
 */
class PebbleTemplateProcessorTest {

    @TempDir
    Path templatesDir;

    private PebbleTemplateProcessor processor;

    @BeforeEach
    void setUp() throws IOException {
        Files.writeString(templatesDir.resolve("hello.html"), "Hello {{ name }}!");
        Files.writeString(templatesDir.resolve("broken.html"), "{% if %}oops");
        processor = new PebbleTemplateProcessor(Map.of("site", "facet"), true, false,
                Locale.ENGLISH, templatesDir.toString());
    }

    @AfterEach
    void tearDown() {
        processor.shutdown();
    }

    @Test
    @DisplayName("templateExists is true for an existing extensionless template")
    void templateExistsTrue() {
        assertTrue(processor.templateExists("hello"));
    }

    @Test
    @DisplayName("templateExists is false for a missing template")
    void templateExistsFalse() {
        assertFalse(processor.templateExists("missing"));
    }

    @Test
    @DisplayName("templateExists is false for a name in a missing subfolder")
    void templateExistsMissingSubfolder() {
        assertFalse(processor.templateExists("nope/hello"));
    }

    @Test
    @DisplayName("process renders context variables")
    void processRenders() throws TemplateProcessingException {
        assertEquals("Hello World!", processor.process("hello", new HashMap<>(Map.of("name", "World"))));
    }

    @Test
    @DisplayName("process of a missing template throws TemplateProcessingException")
    void processMissingThrows() {
        assertThrows(TemplateProcessingException.class, () -> processor.process("missing", new HashMap<>()));
    }

    @Test
    @DisplayName("process of a template with a syntax error throws TemplateProcessingException")
    void processSyntaxErrorThrows() {
        assertThrows(TemplateProcessingException.class, () -> processor.process("broken", new HashMap<>()));
    }

    @Test
    @DisplayName("getGlobalTemplateContext returns a copy")
    void globalContextIsCopy() {
        try {
            processor.getGlobalTemplateContext().put("x", "y");
        } catch (UnsupportedOperationException ignored) {
            // an immutable copy is also acceptable
        }
        assertFalse(processor.getGlobalTemplateContext().containsKey("x"));
        assertEquals("facet", processor.getGlobalTemplateContext().get("site"));
    }

    @Test
    @DisplayName("addToGlobalTemplateContext is visible afterwards")
    void addToGlobalContext() {
        processor.addToGlobalTemplateContext("k", "v");
        assertEquals("v", processor.getGlobalTemplateContext().get("k"));
    }
}
