package com.acme.modres.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class JsonInputStreamTest {

    @TempDir
    Path tempDir;

    static class TestData {
        String name;
        int value;
        TestData(String name, int value) { this.name = name; this.value = value; }
        TestData() {}
    }

    @Test
    void testParseJsonAs_ValidJson() throws IOException {
        File file = new File(tempDir.toFile(), "test.json");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("{\"name\":\"test\", \"value\":123}");
        }
        
        JsonInputStream jis = new JsonInputStream(file);
        TestData result = (TestData) jis.parseJsonAs(TestData.class);
        
        assertNotNull(result);
        assertEquals("test", result.name);
        assertEquals(123, result.value);
    }

    @Test
    void testParseJsonAs_InvalidJson() throws IOException {
        File file = new File(tempDir.toFile(), "invalid.json");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("invalid json");
        }
        
        JsonInputStream jis = new JsonInputStream(file);
        Object result = jis.parseJsonAs(TestData.class);
        
        // Based on implementation, it catches Exception and returns null or the object
        // In case of Gson failure, it might return null or throw.
        // The current implementation catches Exception and returns null if it fails before returning.
        assertNull(result);
    }

    @Test
    void testParseJsonAs_NonExistentFile() throws IOException {
        File file = new File(tempDir.toFile(), "nonexistent.json");
        // We can't instantiate JsonInputStream with a non-existent file easily because of FileNotFoundException
        // but we can test the behavior if we could.
        // Since constructor throws FileNotFoundException, we test that.
        assertThrows(java.io.FileNotFoundException.class, () -> {
            new JsonInputStream(file);
        });
    }
}
