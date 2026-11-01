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
 * Natively supports standard JARs, Spring Boot executable fat JARs (BOOT-INF/classes), WARs (WEB-INF/classes),
 * and Companion Sources JARs (-sources.jar) for reading original Javadoc and comments.
 */
public class ZipBytecodeLoader {

    private static final String[] ARCHIVE_CLASS_PREFIXES = {"", "BOOT-INF/classes/", "WEB-INF/classes/"};

    private static final Set<String> BINARY_EXTENSIONS = Set.of(
            ".png", ".jpg", ".jpeg", ".gif", ".webp", ".ico", ".svgz",
            ".class", ".so", ".dll", ".dylib", ".exe", ".bin", ".dat",
            ".zip", ".tar", ".gz", ".bz2", ".7z", ".pdf", ".ttf", ".woff", ".woff2"
    );

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
                    String cleanName = name;
                    for (String prefix : ARCHIVE_CLASS_PREFIXES) {
                        if (!prefix.isEmpty() && cleanName.startsWith(prefix)) {
                            cleanName = cleanName.substring(prefix.length());
                            break;
                        }
                    }

                    String className = cleanName.substring(0, cleanName.length() - 6);
                    if (filterPattern == null || filterPattern.isEmpty()) {
                        classes.add(className);
                    } else {
                        String dottedName = className.replace('/', '.');
                        if (className.contains(slashFilter) || dottedName.contains(dotFilter) || name.contains(slashFilter)) {
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
        } else if (cleanPath.endsWith(".java")) {
            cleanPath = cleanPath.substring(0, cleanPath.length() - 5);
        }
        String normalizedPath = cleanPath.replace('.', '/') + ".class";

        try (ZipFile zipFile = new ZipFile(archiveFile)) {
            // 1. Direct and prefix lookups (standard, Spring Boot BOOT-INF, WAR WEB-INF)
            for (String prefix : ARCHIVE_CLASS_PREFIXES) {
                String fullPath = prefix + normalizedPath;
                ZipEntry entry = zipFile.getEntry(fullPath);
                if (entry != null) {
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        return is.readAllBytes();
                    }
                }
            }

            // 2. Case-insensitive fallback
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                String name = e.getName();
                for (String prefix : ARCHIVE_CLASS_PREFIXES) {
                    if (name.equalsIgnoreCase(prefix + normalizedPath)) {
                        try (InputStream is = zipFile.getInputStream(e)) {
                            return is.readAllBytes();
                        }
                    }
                }
            }

            throw new IOException("Class not found: " + classPath + " in archive: " + archiveFile.getAbsolutePath());
        }
    }

    /**
     * Attempts to find a companion `-sources.jar` located alongside the binary JAR
     * (e.g. in Maven repository or Gradle cache).
     *
     * @param archiveFile the binary jar file
     * @return the companion sources jar file, or null if not found
     */
    public static File findCompanionSourcesJar(File archiveFile) {
        if (archiveFile == null || !archiveFile.exists() || !archiveFile.isFile()) {
            return null;
        }

        String name = archiveFile.getName();
        if (name.endsWith("-sources.jar")) {
            return archiveFile;
        }

        File parent = archiveFile.getParentFile();
        if (parent == null || !parent.exists()) {
            return null;
        }

        if (name.endsWith(".jar")) {
            String base = name.substring(0, name.length() - 4);

            // 1. Check <name>-sources.jar in the same directory (standard Maven convention)
            File companion = new File(parent, base + "-sources.jar");
            if (companion.exists() && companion.isFile()) {
                return companion;
            }

            // 2. Check Gradle cache structure where files are in sibling hash directories:
            // e.g. caches/modules-2/files-2.1/<group>/<artifact>/<version>/<hash1>/<artifact>-<version>.jar
            // and  caches/modules-2/files-2.1/<group>/<artifact>/<version>/<hash2>/<artifact>-<version>-sources.jar
            File versionDir = parent.getParentFile();
            if (versionDir != null && versionDir.isDirectory()) {
                File[] siblingDirs = versionDir.listFiles(File::isDirectory);
                if (siblingDirs != null) {
                    for (File sibling : siblingDirs) {
                        File candidate = new File(sibling, base + "-sources.jar");
                        if (candidate.exists() && candidate.isFile()) {
                            return candidate;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Reads original Java source code (with Javadoc and comments intact) from companion sources jar.
     *
     * @param sourcesJar sources JAR file
     * @param classPath  class path (e.g. org/example/MyService.class or MyService.java)
     * @return source code with Javadoc, or null if entry not found
     */
    public static String readSourceFromSourcesJar(File sourcesJar, String classPath) {
        if (sourcesJar == null || !sourcesJar.exists()) return null;

        String clean = classPath.replace('\\', '/').replaceAll("^/+", "");
        if (clean.endsWith(".class")) {
            clean = clean.substring(0, clean.length() - 6);
        } else if (clean.endsWith(".java")) {
            clean = clean.substring(0, clean.length() - 5);
        }

        // Top-level class name (strip inner class suffix if any, since inner classes live inside outer .java file)
        int lastSlash = clean.lastIndexOf('/');
        int dollarIdx = clean.indexOf('$', Math.max(0, lastSlash));
        String topLevelClean = dollarIdx > 0 ? clean.substring(0, dollarIdx) : clean;
        String javaEntryPath = topLevelClean.replace('.', '/') + ".java";

        try (ZipFile zip = new ZipFile(sourcesJar)) {
            ZipEntry entry = zip.getEntry(javaEntryPath);
            if (entry == null) {
                // Case-insensitive lookup
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry e = entries.nextElement();
                    if (e.getName().equalsIgnoreCase(javaEntryPath)) {
                        entry = e;
                        break;
                    }
                }
            }

            if (entry != null) {
                try (InputStream is = zip.getInputStream(entry)) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        } catch (IOException ignored) {}

        return null;
    }

    /**
     * Reads text content of any non-class resource file from archive with optional line offset/limit.
     * Safely guards against dumping unreadable binary assets (e.g. .png, .ico, .dll) into AI context.
     */
    public static String readResourceText(File archiveFile, String resourcePath, Integer lineOffset, Integer lineLimit) throws IOException {
        String normalized = resourcePath.replace('\\', '/').replaceAll("^/+", "");

        // Binary extension check to protect AI context window
        String lower = normalized.toLowerCase();
        for (String ext : BINARY_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                long size = -1;
                try (ZipFile zipFile = new ZipFile(archiveFile)) {
                    ZipEntry entry = zipFile.getEntry(normalized);
                    if (entry != null) size = entry.getSize();
                } catch (Exception ignored) {}
                String sizeStr = size >= 0 ? " (" + size + " bytes)" : "";
                return String.format("// [Binary asset: %s%s. Not a text resource. Use extract_archive to save to disk.]", normalized, sizeStr);
            }
        }

        try (ZipFile zipFile = new ZipFile(archiveFile)) {
            // 1. Direct and prefix lookups
            ZipEntry entry = null;
            for (String prefix : ARCHIVE_CLASS_PREFIXES) {
                String fullPath = prefix + normalized;
                entry = zipFile.getEntry(fullPath);
                if (entry != null) break;
            }

            // 2. Case-insensitive fallback
            if (entry == null) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry e = entries.nextElement();
                    String name = e.getName();
                    for (String prefix : ARCHIVE_CLASS_PREFIXES) {
                        if (name.equalsIgnoreCase(prefix + normalized)) {
                            entry = e;
                            break;
                        }
                    }
                    if (entry != null) break;
                }
            }

            if (entry == null) {
                throw new IOException("Resource not found: '" + resourcePath + "' in archive: " + archiveFile.getAbsolutePath());
            }

            String fullText;
            try (InputStream is = zipFile.getInputStream(entry)) {
                fullText = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            return paginateLines(fullText, lineOffset, lineLimit);
        }
    }

    public static String readResourceText(File archiveFile, String resourcePath) throws IOException {
        return readResourceText(archiveFile, resourcePath, null, null);
    }

    /**
     * Paginates text by 1-based start line and line limit.
     */
    public static String paginateLines(String text, Integer lineOffset, Integer lineLimit) {
        if (text == null || (lineOffset == null && lineLimit == null)) {
            return text;
        }

        String[] lines = text.split("\r?\n", -1);
        int totalLines = lines.length;
        int start = (lineOffset != null && lineOffset > 0) ? lineOffset - 1 : 0;
        if (start >= totalLines) {
            return "// Offset line " + lineOffset + " exceeds total line count (" + totalLines + ").";
        }

        int limit = (lineLimit != null && lineLimit > 0) ? lineLimit : totalLines - start;
        int end = Math.min(start + limit, totalLines);

        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            sb.append(lines[i]);
            if (i < end - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Inspects archive hierarchy with custom root folder filter and max depth.
     */
    public static ArchiveTree inspectTree(File archiveFile, String subPath, Integer maxDepth) throws IOException {
        Map<String, Long> entriesMap = new LinkedHashMap<>();
        try (ZipFile zipFile = new ZipFile(archiveFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                entriesMap.put(entry.getName(), entry.getSize() >= 0 ? entry.getSize() : 0);
            }
        }
        return ArchiveTree.build(archiveFile.getName(), archiveFile.length(), entriesMap, subPath, maxDepth);
    }

    public static ArchiveTree inspectTree(File archiveFile) throws IOException {
        return inspectTree(archiveFile, null, 3);
    }

    /**
     * Extracts files from archive to a target directory.
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
