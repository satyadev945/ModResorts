package com.acme.modres.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class ZipValidatorTest {

    @TempDir
    Path tempDir;

    @Test
    void testIsValid_WithValidZip() throws Throwable {
        File zipFile = createZipFile("test.zip", true);
        ZipValidator validator = new ZipValidator(zipFile);
        // Note: The original isValid implementation has a bug where it creates a new ZipValidator 
        // inside isValid, which might lead to issues or infinite recursion if not careful.
        // However, we test the current implementation.
        assertTrue(validator.isValid());
    }

    @Test
    void testIsValid_WithEmptyZip() throws Throwable {
        File zipFile = createZipFile("empty.zip", false);
        ZipValidator validator = new ZipValidator(zipFile);
        assertTrue(validator.isValid());
    }

    @Test
    void testIsValid_WithNonExistentFile() throws Throwable {
        File nonExistentFile = new File(tempDir.toFile(), "nonexistent.zip");
        ZipValidator validator = new ZipValidator(nonExistentFile);
        assertFalse(validator.isValid());
    }

    private File createZipFile(String fileName, boolean addEntry) throws IOException {
        File file = new File(tempDir.toFile(), fileName);
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file))) {
            if (addEntry) {
                ZipEntry entry = new ZipEntry("test.txt");
                zos.putNextEntry(entry);
                zos.write("test content".getBytes());
                zos.closeEntry();
            }
        }
        return file;
    }
}
