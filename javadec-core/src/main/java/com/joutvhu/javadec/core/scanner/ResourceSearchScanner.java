package com.joutvhu.javadec.core.scanner;

import com.joutvhu.javadec.core.model.ResourceSearchMatch;
import com.joutvhu.javadec.core.model.ResourceSearchResult;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Searches across all non-class configuration and resource files (.properties, .yml, .xml, .json, .factories, etc.)
 * in a JAR archive for text keywords or patterns in-memory.
 */
public class ResourceSearchScanner {

    private static final String[] ARCHIVE_PREFIXES = {"", "BOOT-INF/classes/", "WEB-INF/classes/"};

    private static final Set<String> BINARY_EXTENSIONS = Set.of(
            ".png", ".jpg", ".jpeg", ".gif", ".webp", ".ico", ".svgz",
            ".class", ".so", ".dll", ".dylib", ".exe", ".bin", ".dat",
            ".zip", ".tar", ".gz", ".bz2", ".7z", ".pdf", ".ttf", ".woff", ".woff2", ".jar"
    );

    public static ResourceSearchResult search(File archiveFile, String query, Integer limit, String pathFilter) throws IOException {
        if (query == null || query.isBlank()) {
            return ResourceSearchResult.of(archiveFile.getName(), "", List.of());
        }

        int max = (limit != null && limit > 0) ? limit : 100;
        String lowerQuery = query.trim().toLowerCase();
        String cleanPathFilter = pathFilter != null ? pathFilter.replace('\\', '/').replaceAll("^/+", "").replace('.', '/') : null;

        List<ResourceSearchMatch> matches = new ArrayList<>();

        try (ZipFile zipFile = new ZipFile(archiveFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements() && matches.size() < max) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || isBinary(name)) continue;

                String cleanName = name;
                for (String pfx : ARCHIVE_PREFIXES) {
                    if (!pfx.isEmpty() && cleanName.startsWith(pfx)) {
                        cleanName = cleanName.substring(pfx.length());
                        break;
                    }
                }

                if (cleanPathFilter != null && !cleanName.startsWith(cleanPathFilter) && !cleanName.contains(cleanPathFilter)) {
                    continue;
                }

                try (InputStream is = zipFile.getInputStream(entry);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    int lineNum = 0;
                    while ((line = reader.readLine()) != null && matches.size() < max) {
                        lineNum++;
                        if (line.toLowerCase().contains(lowerQuery)) {
                            // Strip BOM on first line if present
                            if (lineNum == 1 && line.startsWith("\uFEFF")) {
                                line = line.substring(1);
                            }
                            matches.add(new ResourceSearchMatch(cleanName, lineNum, line.trim()));
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        return ResourceSearchResult.of(archiveFile.getName(), query, matches);
    }

    private static boolean isBinary(String name) {
        String lower = name.toLowerCase();
        for (String ext : BINARY_EXTENSIONS) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }
}
