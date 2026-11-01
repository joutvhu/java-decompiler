package com.joutvhu.javadec.core.loader;

import com.joutvhu.javadec.core.model.ArchiveTree;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * High-performance reader and extractor for JAR/ZIP archives and in-memory bytecode/resources.
 */
public class ZipBytecodeLoader {

    public static List<String> listClasses(File archiveFile, String filterPattern) throws IOException {
        List<String> classes = new ArrayList<>();
        String slashFilter = filterPattern != null ? filterPattern.trim().replace('\\', '/').replace('.', '/') : null;
        String dotFilter = filterPattern != null ? filterPattern.trim().replace('\\', '.').replace('/', '.') : null;

        try (ZipFile zipFile = new ZipFile(archiveFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.endsWith(".class") && !entry.isDirectory()) {
                    String className = name.substring(0, name.length() - 6);
                    if (filterPattern == null || filterPattern.isEmpty()) {
                        classes.add(className);
                    } else {
                        String dottedName = className.replace('/', '.');
                        if (className.contains(slashFilter) || dottedName.contains(dotFilter)) {
                            classes.add(className);
                        }
                    }
                }
            }
        }
        return classes;
    }

    public static byte[] loadClassBytes(File archiveFile, String classPath) throws IOException {
        String cleanPath = classPath.replace('\\', '/');
        if (cleanPath.endsWith(".class")) {
            cleanPath = cleanPath.substring(0, cleanPath.length() - 6);
        }
        String normalizedPath = cleanPath.replace('.', '/') + ".class";

        try (ZipFile zipFile = new ZipFile(archiveFile)) {
            ZipEntry entry = zipFile.getEntry(normalizedPath);
            if (entry == null) {
                // Try case-insensitive lookup as fallback
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry e = entries.nextElement();
                    if (e.getName().equalsIgnoreCase(normalizedPath)) {
                        entry = e;
                        break;
                    }
                }
            }

            if (entry == null) {
                throw new IOException("Class not found: " + classPath + " in archive: " + archiveFile.getAbsolutePath());
            }

            try (InputStream is = zipFile.getInputStream(entry)) {
                return is.readAllBytes();
            }
        }
    }

    /**
     * Reads text content of any non-class resource file (e.g. .yml, .properties, .xml, MANIFEST.MF) from archive.
     */
    public static String readResourceText(File archiveFile, String resourcePath) throws IOException {
        String normalized = resourcePath.replace('\\', '/').replaceAll("^/+", "");
        try (ZipFile zipFile = new ZipFile(archiveFile)) {
            ZipEntry entry = zipFile.getEntry(normalized);
            if (entry == null) {
                // Case-insensitive lookup
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry e = entries.nextElement();
                    if (e.getName().equalsIgnoreCase(normalized)) {
                        entry = e;
                        break;
                    }
                }
            }

            if (entry == null) {
                throw new IOException("Resource not found: '" + resourcePath + "' in archive: " + archiveFile.getAbsolutePath());
            }

            try (InputStream is = zipFile.getInputStream(entry)) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    /**
     * Inspects archive hierarchy and returns a compact structured ArchiveTree.
     */
    public static ArchiveTree inspectTree(File archiveFile) throws IOException {
        Map<String, Long> entriesMap = new LinkedHashMap<>();
        try (ZipFile zipFile = new ZipFile(archiveFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                entriesMap.put(entry.getName(), entry.getSize() >= 0 ? entry.getSize() : 0);
            }
        }
        return ArchiveTree.build(archiveFile.getName(), archiveFile.length(), entriesMap);
    }

    /**
     * Extracts files from archive to a target directory.
     *
     * @param archiveFile the JAR/ZIP file
     * @param targetDir   destination directory
     * @param pathFilter  optional prefix or file filter (null extracts all)
     * @return number of files extracted
     */
    public static int extractArchive(File archiveFile, File targetDir, String pathFilter) throws IOException {
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        int count = 0;
        String filter = pathFilter != null ? pathFilter.replace('\\', '/').replaceAll("^/+", "") : null;

        try (ZipFile zipFile = new ZipFile(archiveFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();

                if (filter != null && !name.startsWith(filter) && !name.contains(filter)) {
                    continue;
                }

                File destFile = new File(targetDir, name);
                // Security check against zip-slip vulnerability
                Path destPath = destFile.toPath().normalize();
                if (!destPath.startsWith(targetDir.toPath().normalize())) {
                    throw new IOException("Bad zip entry (Zip Slip): " + name);
                }

                if (entry.isDirectory()) {
                    destFile.mkdirs();
                } else {
                    destFile.getParentFile().mkdirs();
                    try (InputStream is = zipFile.getInputStream(entry);
                         OutputStream os = Files.newOutputStream(destPath)) {
                        is.transferTo(os);
                    }
                    count++;
                }
            }
        }
        return count;
    }
}
