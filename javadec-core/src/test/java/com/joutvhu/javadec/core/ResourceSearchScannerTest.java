package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.model.ResourceSearchResult;
import com.joutvhu.javadec.core.scanner.ResourceSearchScanner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class ResourceSearchScannerTest {

    @Test
    public void testSearchTextAcrossConfigurations(@TempDir Path tempDir) throws Exception {
        File jar = tempDir.resolve("res-test.jar").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(jar))) {
            // 1. application.yml
            zos.putNextEntry(new ZipEntry("application.yml"));
            String yml = """
                    server:
                      port: 8080
                    spring:
                      datasource:
                        url: jdbc:postgresql://localhost:5432/mydb
                        password: mysecretpassword
                    """;
            zos.write(yml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 2. META-INF/spring.factories
            zos.putNextEntry(new ZipEntry("META-INF/spring.factories"));
            String factories = """
                    org.springframework.boot.autoconfigure.EnableAutoConfiguration=\\
                      com.example.MyAutoConfiguration
                    """;
            zos.write(factories.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 3. Binary file that should be skipped
            zos.putNextEntry(new ZipEntry("static/icon.png"));
            zos.write(new byte[]{1, 2, 3});
            zos.closeEntry();
        }

        // Search for password
        ResourceSearchResult res1 = ResourceSearchScanner.search(jar, "password", 10, null);
        assertNotNull(res1);
        assertEquals(1, res1.totalMatches());
        assertEquals("application.yml", res1.matches().get(0).resourcePath());
        assertTrue(res1.matches().get(0).lineContent().contains("mysecretpassword"));

        // Search for configuration
        ResourceSearchResult res2 = ResourceSearchScanner.search(jar, "AutoConfiguration", 10, null);
        assertNotNull(res2);
        assertEquals(2, res2.totalMatches()); // matches line 1 (EnableAutoConfiguration) and line 2 (MyAutoConfiguration)
        assertEquals("META-INF/spring.factories", res2.matches().get(0).resourcePath());
    }
}
