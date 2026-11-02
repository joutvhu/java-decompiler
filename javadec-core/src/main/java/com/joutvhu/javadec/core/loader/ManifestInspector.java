package com.joutvhu.javadec.core.loader;

import com.joutvhu.javadec.core.model.ManifestSummary;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Inspects and parses structured packaging metadata, manifest attributes, and SPI services from an archive.
 */
public class ManifestInspector {

    public static ManifestSummary inspect(File archiveFile) throws Exception {
        Map<String, String> keyAttrs = new LinkedHashMap<>();
        Map<String, List<String>> spiServices = new TreeMap<>();
        boolean isMultiRelease = false;
        String packagingType = "Standard JAR";
        String mainClass = null;
        String startClass = null;
        String buildJdk = null;
        String createdBy = null;

        try (ZipFile zipFile = new ZipFile(archiveFile)) {
            // 1. Read MANIFEST.MF
            ZipEntry manifestEntry = zipFile.getEntry("META-INF/MANIFEST.MF");
            if (manifestEntry != null) {
                try (InputStream is = zipFile.getInputStream(manifestEntry)) {
                    Manifest manifest = new Manifest(is);
                    Attributes main = manifest.getMainAttributes();

                    mainClass = main.getValue("Main-Class");
                    startClass = main.getValue("Start-Class");
                    buildJdk = main.getValue("Build-Jdk-Spec");
                    if (buildJdk == null) buildJdk = main.getValue("Build-Jdk");
                    createdBy = main.getValue("Created-By");
                    isMultiRelease = "true".equalsIgnoreCase(main.getValue("Multi-Release"));

                    for (Map.Entry<Object, Object> entry : main.entrySet()) {
                        keyAttrs.put(entry.getKey().toString(), entry.getValue().toString());
                    }
                }
            }

            // 2. Scan SPI services under META-INF/services/
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            boolean hasBootInf = false;
            boolean hasWebInf = false;

            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                String name = e.getName();

                if (name.startsWith("BOOT-INF/")) hasBootInf = true;
                if (name.startsWith("WEB-INF/")) hasWebInf = true;

                if (name.startsWith("META-INF/services/") && !e.isDirectory()) {
                    String serviceInterface = name.substring("META-INF/services/".length());
                    if (!serviceInterface.isEmpty() && !serviceInterface.contains("/")) {
                        List<String> providers = new ArrayList<>();
                        try (InputStream is = zipFile.getInputStream(e);
                             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                line = line.trim();
                                if (!line.isEmpty() && !line.startsWith("#")) {
                                    providers.add(line);
                                }
                            }
                        } catch (Exception ignored) {}
                        if (!providers.isEmpty()) {
                            spiServices.put(serviceInterface, providers);
                        }
                    }
                }
            }

            if (hasBootInf) {
                packagingType = "Spring Boot Executable Archive (BOOT-INF)";
            } else if (hasWebInf) {
                packagingType = "Web Application Archive (WAR)";
            } else if (keyAttrs.containsKey("Bundle-SymbolicName")) {
                packagingType = "OSGi Bundle (" + keyAttrs.get("Bundle-SymbolicName") + ")";
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Packaging & Manifest: ").append(archiveFile.getName()).append("\n");
        sb.append("- Type: ").append(packagingType).append("\n");
        if (mainClass != null) sb.append("- Main-Class: ").append(mainClass).append("\n");
        if (startClass != null) sb.append("- Start-Class: ").append(startClass).append("\n");
        if (buildJdk != null) sb.append("- Build JDK: ").append(buildJdk).append("\n");
        if (createdBy != null) sb.append("- Created-By: ").append(createdBy).append("\n");
        sb.append("- Multi-Release: ").append(isMultiRelease).append("\n");

        if (!spiServices.isEmpty()) {
            sb.append("\nSPI Services (META-INF/services):\n");
            for (Map.Entry<String, List<String>> entry : spiServices.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(":\n");
                for (String prov : entry.getValue()) {
                    sb.append("    \\-- ").append(prov).append("\n");
                }
            }
        }

        return new ManifestSummary(
                archiveFile.getName(),
                packagingType,
                mainClass,
                startClass,
                buildJdk,
                createdBy,
                isMultiRelease,
                keyAttrs,
                spiServices,
                sb.toString().trim()
        );
    }
}
