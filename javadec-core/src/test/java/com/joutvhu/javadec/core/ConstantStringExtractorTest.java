package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.model.ConstantStringResult;
import com.joutvhu.javadec.core.scanner.ConstantStringExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class ConstantStringExtractorTest {

    @Test
    public void testExtractHardcodedStrings(@TempDir Path tempDir) throws Exception {
        String src = """
                package com.example;
                public class ApiClient {
                    private static final String API_URL = "https://api.example.com/v1";
                    public void query() {
                        String sql = "SELECT * FROM users WHERE active = 1";
                        String errMsg = "Connection timeout occurred";
                    }
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.ApiClient", src);
        File jar = tempDir.resolve("strings-test.jar").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(jar))) {
            for (var entry : classes.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey().replace('.', '/') + ".class"));
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }

        // 1. Extract all strings
        ConstantStringResult all = ConstantStringExtractor.extractFromArchive(jar, null, null, 3, 50);
        assertTrue(all.totalFound() >= 3);
        boolean hasUrl = all.items().stream().anyMatch(i -> i.value().contains("https://api.example.com"));
        boolean hasSql = all.items().stream().anyMatch(i -> i.value().contains("SELECT * FROM"));
        assertTrue(hasUrl);
        assertTrue(hasSql);

        // 2. Filter by text substring
        ConstantStringResult filtered = ConstantStringExtractor.extractFromArchive(jar, null, "SELECT", 3, 50);
        assertEquals(1, filtered.totalFound());
        assertTrue(filtered.items().get(0).value().startsWith("SELECT"));

        String summary = all.formattedSummary();
        assertTrue(summary.contains("ApiClient"));
        assertTrue(summary.contains("https://api.example.com/v1"));
    }
}
