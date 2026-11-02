package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.loader.ManifestInspector;
import com.joutvhu.javadec.core.model.ManifestSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class ManifestInspectorTest {

    @Test
    public void testInspectManifestAndPackaging(@TempDir Path tempDir) throws Exception {
        File jar = tempDir.resolve("boot-sample.jar").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(jar))) {
            // Write MANIFEST.MF
            zos.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            String manifestText = """
                    Manifest-Version: 1.0
                    Main-Class: org.springframework.boot.loader.JarLauncher
                    Start-Class: com.example.Application
                    Build-Jdk-Spec: 21
                    Created-By: Maven JAR Plugin 3.3.0
                    Multi-Release: true
                    
                    """;
            zos.write(manifestText.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Write BOOT-INF entry to simulate Spring Boot fat jar
            zos.putNextEntry(new ZipEntry("BOOT-INF/classes/com/example/Application.class"));
            zos.write(new byte[]{1, 2, 3});
            zos.closeEntry();

            // Write SPI service
            zos.putNextEntry(new ZipEntry("META-INF/services/com.example.Plugin"));
            zos.write("com.example.PluginImpl\n".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        ManifestSummary summary = ManifestInspector.inspect(jar);
        assertNotNull(summary);
        assertTrue(summary.packagingType().contains("Spring Boot"));
        assertEquals("org.springframework.boot.loader.JarLauncher", summary.mainClass());
        assertEquals("com.example.Application", summary.startClass());
        assertEquals("21", summary.buildJdk());
        assertTrue(summary.isMultiRelease());
        assertTrue(summary.spiServices().containsKey("com.example.Plugin"));

        String formatted = summary.formattedSummary();
        assertTrue(formatted.contains("Spring Boot"));
        assertTrue(formatted.contains("com.example.Plugin"));
    }
}
