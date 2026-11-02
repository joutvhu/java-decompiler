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
 * Automatically resolves inner classes written with dot notation (e.g. Outer.Inner -> Outer$Inner),
 * and supports case-insensitive search patterns.
 */
public class ZipBytecodeLoader {

    private static final String[] ARCHIVE_CLASS_PREFIXES = {"", "BOOT-INF/classes/", "WEB-INF/classes/"};

    private static final Set<String> BINARY_EXTENSIONS = Set.of(
            ".png", ".jpg", ".jpeg", ".gif", ".webp", ".ico", ".svgz",
            ".class", ".so", ".dll", ".dylib", ".exe", ".bin", ".dat",
            ".zip", ".tar", ".gz", ".bz2", ".7z", ".pdf", ".ttf", ".woff", ".woff2"
    );

    public static List<String> listClasses(File archiveFile, String filterPattern) throws IOException {
        return listClasses(archiveFile, filterPattern, null);
    }

    public static List<String> listClasses(File archiveFile, String filterPattern, String annotationFilter) throws IOException {
        List<String> classes = new ArrayList<>();

        String cleanFilter = filterPattern != null ? filterPattern.trim().replaceAll("^\\*+", "").replaceAll("\\*+$", "") : null;
        String slashFilter = cleanFilter != null ? cleanFilter.replace('\\', '/').replace('.', '/') : null;
        String dotFilter = cleanFilter != null ? cleanFilter.replace('\\', '.').replace('/', '.') : null;

        String lowerSlashFilter = slashFilter != null ? slashFilter.toLowerCase() : null;
        String lowerDotFilter = dotFilter != null ? dotFilter.toLowerCase() : null;

        String cleanAnn = (annotationFilter != null && !annotationFilter.isBlank())
                ? annotationFilter.trim().replaceAll("^@+", "").replace('/', '.')
                : null;
        String simpleAnn = cleanAnn != null ? (cleanAnn.contains(".") ? cleanAnn.substring(cleanAnn.lastIndexOf('.') + 1) : cleanAnn) : null;

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
                    boolean nameMatches = true;
                    if (cleanFilter != null && !cleanFilter.isEmpty()) {
                        String dottedName = className.replace('/', '.');
                        String lowerClass = className.toLowerCase();
                        String lowerDotted = dottedName.toLowerCase();
                        String lowerRaw = name.toLowerCase();

                        nameMatches = (lowerClass.contains(lowerSlashFilter) || lowerDotted.contains(lowerDotFilter) || lowerRaw.contains(lowerSlashFilter));
                    }

                    if (!nameMatches) continue;

                    // Filter by annotation if requested
                    if (cleanAnn != null) {
                        try (InputStream is = zipFile.getInputStream(entry)) {
                            byte[] bytes = is.readAllBytes();
                            if (!classHasAnnotation(bytes, cleanAnn, simpleAnn)) {
                                continue;
                            }
                        } catch (Exception ignored) {
                            continue;
                        }
                    }

                    classes.add(className);
                }
            }
        }
        return classes;
    }

    private static boolean classHasAnnotation(byte[] bytecode, String fullAnn, String simpleAnn) {
        if (bytecode == null || bytecode.length < 8) return false;
        // Fast pre-check: constant pool must contain the annotation simple name
        String content = new String(bytecode, StandardCharsets.ISO_8859_1);
        if (!content.contains(simpleAnn)) return false;

        byte[] compatible = bytecode;
        int majorVersion = ((bytecode[6] & 0xFF) << 8) | (bytecode[7] & 0xFF);
        if (majorVersion > 65) {
            compatible = bytecode.clone();
            compatible[6] = 0;
            compatible[7] = 65;
        }

        final boolean[] found = {false};
        try {
            org.objectweb.asm.ClassReader reader = new org.objectweb.asm.ClassReader(compatible);
            reader.accept(new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
                @Override
                public org.objectweb.asm.AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    checkAnn(descriptor);
                    return null;
                }

                @Override
                public org.objectweb.asm.FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                    return new org.objectweb.asm.FieldVisitor(org.objectweb.asm.Opcodes.ASM9) {
                        @Override
                        public org.objectweb.asm.AnnotationVisitor visitAnnotation(String desc, boolean vis) {
                            checkAnn(desc);
                            return null;
                        }
                    };
                }

                @Override
                public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    return new org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9) {
                        @Override
                        public org.objectweb.asm.AnnotationVisitor visitAnnotation(String desc, boolean vis) {
                            checkAnn(desc);
                            return null;
                        }
                    };
                }

                private void checkAnn(String desc) {
                    if (desc != null) {
                        String annType = org.objectweb.asm.Type.getType(desc).getClassName();
                        if (annType.equals(fullAnn) || annType.endsWith("." + simpleAnn) || annType.equals(simpleAnn)) {
                            found[0] = true;
                        }
                    }
                }
            }, org.objectweb.asm.ClassReader.SKIP_CODE | org.objectweb.asm.ClassReader.SKIP_DEBUG | org.objectweb.asm.ClassReader.SKIP_FRAMES);
        } catch (Exception ignored) {}

        return found[0];
    }

    public static byte[] loadClassBytes(File archiveFile, String classPath) throws IOException {
        String cleanPath = classPath.replace('\\', '/').replaceAll("^/+", "");

        // Strip pre-existing Spring Boot / WAR prefix to avoid doubling prefix during lookups
        for (String prefix : ARCHIVE_CLASS_PREFIXES) {
            if (!prefix.isEmpty() && cleanPath.startsWith(prefix)) {
                cleanPath = cleanPath.substring(prefix.length());
                break;
            }
        }

        if (cleanPath.endsWith(".class")) {
            cleanPath = cleanPath.substring(0, cleanPath.length() - 6);
        } else if (cleanPath.endsWith(".java")) {
            cleanPath = cleanPath.substring(0, cleanPath.length() - 5);
        }
        String normalizedPath = cleanPath.replace('.', '/') + ".class";

        try (ZipFile zipFile = new ZipFile(archiveFile)) {
            ZipEntry entry = findClassZipEntry(zipFile, normalizedPath);
            if (entry != null) {
                try (InputStream is = zipFile.getInputStream(entry)) {
                    return is.readAllBytes();
                }
            }

            throw new IOException("Class not found: " + classPath + " in archive: " + archiveFile.getAbsolutePath());
        }
    }

    /**
     * Resolves a class entry in a ZIP file, supporting:
     * 1. Standard package path (org/example/MyClass.class)
     * 2. Spring Boot / WAR prefixes (BOOT-INF/classes/, WEB-INF/classes/)
     * 3. Inner-class dot-to-dollar resolution (e.g. Outer/Inner.class -> Outer$Inner.class)
     * 4. Case-insensitive lookup as fallback
     */
    private static ZipEntry findClassZipEntry(ZipFile zipFile, String normalizedPath) {
        // 1. Direct and prefix lookups
        for (String prefix : ARCHIVE_CLASS_PREFIXES) {
            String fullPath = prefix + normalizedPath;
            ZipEntry entry = zipFile.getEntry(fullPath);
            if (entry != null) return entry;
        }

        // 2. Inner-class resolution: replace trailing '/' with '$' for inner classes written with dot notation
        List<String> candidates = new ArrayList<>();
        String candidate = normalizedPath;
        int slashIdx;
        while ((slashIdx = candidate.lastIndexOf('/')) > 0) {
            candidate = candidate.substring(0, slashIdx) + "$" + candidate.substring(slashIdx + 1);
            candidates.add(candidate);
            for (String prefix : ARCHIVE_CLASS_PREFIXES) {
                String fullPath = prefix + candidate;
                ZipEntry entry = zipFile.getEntry(fullPath);
                if (entry != null) return entry;
            }
        }

        // 3. Case-insensitive fallback
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry e = entries.nextElement();
            String name = e.getName();
            for (String prefix : ARCHIVE_CLASS_PREFIXES) {
                if (name.equalsIgnoreCase(prefix + normalizedPath)) {
                    return e;
                }
                for (String cand : candidates) {
                    if (name.equalsIgnoreCase(prefix + cand)) {
                        return e;
                    }
                }
            }
        }

        return null;
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

        // Strip Spring Boot / WAR prefixes if passed from a fat jar
        for (String prefix : ARCHIVE_CLASS_PREFIXES) {
            if (!prefix.isEmpty() && clean.startsWith(prefix)) {
                clean = clean.substring(prefix.length());
                break;
            }
        }

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

            // If not found, try stripping sub-class names in case inner class was passed with dots (e.g. Outer.Inner)
            if (entry == null) {
                String candidate = javaEntryPath;
                int dotJavaSlash;
                while ((dotJavaSlash = candidate.lastIndexOf('/', candidate.length() - 6)) > 0) {
                    candidate = candidate.substring(0, dotJavaSlash) + ".java";
                    entry = zip.getEntry(candidate);
                    if (entry != null) break;
                }
            }

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
                    String src = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    if (src.startsWith("\uFEFF")) {
                        src = src.substring(1);
                    }
                    return src;
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

        // Strip pre-existing Spring Boot / WAR prefix to avoid doubling prefix
        for (String prefix : ARCHIVE_CLASS_PREFIXES) {
            if (!prefix.isEmpty() && normalized.startsWith(prefix)) {
                normalized = normalized.substring(prefix.length());
                break;
            }
        }

        // Binary extension check to protect AI context window
        String lower = normalized.toLowerCase();
        for (String ext : BINARY_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                long size = -1;
                try (ZipFile zipFile = new ZipFile(archiveFile)) {
                    for (String prefix : ARCHIVE_CLASS_PREFIXES) {
                        ZipEntry entry = zipFile.getEntry(prefix + normalized);
                        if (entry != null) {
                            size = entry.getSize();
                            break;
                        }
                    }
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

            if (lineOffset != null || lineLimit != null) {
                int start = (lineOffset != null && lineOffset > 0) ? lineOffset : 1;
                int maxCount = (lineLimit != null && lineLimit > 0) ? lineLimit : Integer.MAX_VALUE;
                int end = (maxCount == Integer.MAX_VALUE) ? Integer.MAX_VALUE : start + maxCount - 1;

                StringBuilder sb = new StringBuilder();
                int currentLine = 0;
                try (InputStream is = zipFile.getInputStream(entry);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        currentLine++;
                        if (currentLine >= start && currentLine <= end) {
                            if (currentLine == 1 && line.startsWith("\uFEFF")) {
                                line = line.substring(1);
                            }
                            sb.append(line);
                            if (currentLine < end) {
                                sb.append("\n");
                            }
                        }
                        if (currentLine > end) {
                            break;
                        }
                    }
                }
                if (currentLine < start) {
                    return "// Offset line " + lineOffset + " exceeds total line count (" + currentLine + ").";
                }
                String res = sb.toString();
                if (res.endsWith("\n")) {
                    res = res.substring(0, res.length() - 1);
                }
                return res;
            }

            int maxBytes = 10 * 1024 * 1024;
            if (entry.getSize() > maxBytes) {
                return String.format("// [Resource '%s' is too large (%d bytes). Please specify line offset/limit or use extract_archive to save to disk.]", normalized, entry.getSize());
            }

            String fullText;
            try (InputStream is = zipFile.getInputStream(entry)) {
                byte[] bytes = is.readNBytes(maxBytes + 1);
                if (bytes.length > maxBytes) {
                    return String.format("// [Resource '%s' is too large (> 10MB). Please specify line offset/limit or use extract_archive to save to disk.]", normalized);
                }
                fullText = new String(bytes, StandardCharsets.UTF_8);
                if (fullText.startsWith("\uFEFF")) {
                    fullText = fullText.substring(1);
                }
            }

            return fullText;
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

                String safeName = name.replace('\\', '/').replaceAll("^/+", "");

                if (filter != null && !safeName.startsWith(filter) && !safeName.contains(filter)) {
                    continue;
                }

                File destFile = new File(targetDir, safeName);
                // Security check against zip-slip vulnerability
                Path destPath = destFile.toPath().toAbsolutePath().normalize();
                Path baseTargetPath = targetDir.toPath().toAbsolutePath().normalize();
                if (!destPath.startsWith(baseTargetPath)) {
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
