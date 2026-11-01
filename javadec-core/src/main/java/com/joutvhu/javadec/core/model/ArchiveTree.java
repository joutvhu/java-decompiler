package com.joutvhu.javadec.core.model;

import java.util.*;

/**
 * Represents a hierarchical tree structure of entries in a JAR/ZIP archive.
 * Summarizes packages, classes, and resource files with configurable root path and depth.
 * Uses ASCII-safe tree connectors to guarantee zero-mojibake across all operating systems and LLM context windows.
 */
public record ArchiveTree(
        String archiveName,
        String subPath,
        long totalSize,
        int totalClasses,
        int totalResources,
        List<String> topLevelPackages,
        List<String> keyResources,
        String formattedTree
) {

    private static final int MAX_CHILDREN_PER_DIR = 15;
    private static final List<String> ARCHIVE_PREFIXES = List.of("BOOT-INF/classes/", "WEB-INF/classes/");

    private static final String BRANCH = "|-- ";
    private static final String LEAF   = "\\-- ";
    private static final String PIPE   = "|   ";
    private static final String SPACE  = "    ";

    public static ArchiveTree build(String archiveName, long totalSize, Map<String, Long> entries, String subPath, Integer maxDepth) {
        int depthLimit = (maxDepth != null && maxDepth > 0) ? maxDepth : 3;
        String normalizedSubPath = (subPath != null && !subPath.isBlank())
                ? subPath.trim().replace('\\', '/').replace('.', '/').replaceAll("^/+", "").replaceAll("/+$", "")
                : "";

        int classCount = 0;
        int resourceCount = 0;
        long filteredSize = 0;
        Set<String> packages = new TreeSet<>();
        List<String> resources = new ArrayList<>();

        TreeNode root = new TreeNode("");

        for (Map.Entry<String, Long> entry : entries.entrySet()) {
            String path = entry.getKey().replace('\\', '/');
            if (path.endsWith("/")) continue;

            String cleanPath = path;
            for (String pfx : ARCHIVE_PREFIXES) {
                if (cleanPath.startsWith(pfx)) {
                    cleanPath = cleanPath.substring(pfx.length());
                    break;
                }
            }

            // Filter by subPath if specified (matches against raw path or clean path)
            if (!normalizedSubPath.isEmpty()) {
                boolean matchesPath = path.startsWith(normalizedSubPath + "/") || path.equals(normalizedSubPath);
                boolean matchesClean = cleanPath.startsWith(normalizedSubPath + "/") || cleanPath.equals(normalizedSubPath);
                if (!matchesPath && !matchesClean) {
                    continue;
                }
            }

            filteredSize += entry.getValue();

            if (path.endsWith(".class")) {
                classCount++;
                int lastSlash = cleanPath.lastIndexOf('/');
                if (lastSlash > 0) {
                    String pkg = cleanPath.substring(0, lastSlash).replace('/', '.');
                    int dotIdx = pkg.indexOf('.');
                    packages.add(dotIdx > 0 ? pkg.substring(0, pkg.indexOf('.', dotIdx + 1) > 0 ? pkg.indexOf('.', dotIdx + 1) : pkg.length()) : pkg);
                }
            } else {
                resourceCount++;
                String lower = path.toLowerCase();
                boolean isImageOrBinary = lower.endsWith(".svg") || lower.endsWith(".png") || lower.endsWith(".ico") || lower.endsWith(".jpg");
                if (!isImageOrBinary && (lower.endsWith(".yml") || lower.endsWith(".yaml")
                        || lower.endsWith(".properties") || lower.endsWith(".xml") || lower.endsWith(".json")
                        || lower.endsWith(".sql") || lower.endsWith(".proto") || lower.endsWith(".factories")
                        || lower.endsWith(".imports") || lower.endsWith(".jar") || path.equals("META-INF/MANIFEST.MF"))) {
                    resources.add(path);
                }
            }

            // Build relative hierarchy
            String relativePath;
            if (normalizedSubPath.isEmpty()) {
                relativePath = path;
            } else if (cleanPath.startsWith(normalizedSubPath + "/")) {
                relativePath = cleanPath.substring(normalizedSubPath.length()).replaceAll("^/+", "");
            } else if (cleanPath.equals(normalizedSubPath)) {
                relativePath = cleanPath.substring(Math.max(0, cleanPath.lastIndexOf('/') + 1));
            } else if (path.startsWith(normalizedSubPath + "/")) {
                relativePath = path.substring(normalizedSubPath.length()).replaceAll("^/+", "");
            } else {
                relativePath = path.substring(Math.max(0, path.lastIndexOf('/') + 1));
            }

            if (relativePath.isEmpty()) continue;

            String[] segments = relativePath.split("/");
            TreeNode current = root;
            for (int i = 0; i < segments.length; i++) {
                String segment = segments[i];
                boolean isLeaf = (i == segments.length - 1);
                current = current.children.computeIfAbsent(segment, s -> new TreeNode(s, isLeaf, isLeaf ? entry.getValue() : 0));
            }
        }

        // Sort key resources: prioritize MANIFEST.MF, application config, plugin configs
        resources.sort((a, b) -> {
            int pA = a.contains("MANIFEST.MF") ? 0 : a.contains("application") ? 1 : a.contains("plugin.xml") ? 2 : 3;
            int pB = b.contains("MANIFEST.MF") ? 0 : b.contains("application") ? 1 : b.contains("plugin.xml") ? 2 : 3;
            if (pA != pB) return Integer.compare(pA, pB);
            return a.compareToIgnoreCase(b);
        });

        StringBuilder sb = new StringBuilder();
        String headerTitle = normalizedSubPath.isEmpty() ? archiveName : archiveName + "!" + normalizedSubPath;
        sb.append(headerTitle).append(" (");
        if (normalizedSubPath.isEmpty()) {
            sb.append(formatBytes(totalSize));
        } else {
            sb.append(formatBytes(filteredSize)).append(" of ").append(formatBytes(totalSize));
        }
        sb.append(", ")
                .append(classCount).append(" classes, ")
                .append(resourceCount).append(" resources, depth=").append(depthLimit).append(")\n");

        if (normalizedSubPath.isEmpty()) {
            if (!packages.isEmpty()) {
                sb.append("Packages: ").append(String.join(", ", packages)).append("\n");
            }
            if (!resources.isEmpty()) {
                List<String> displayConfigs = resources.size() > 5 ? resources.subList(0, 5) : resources;
                sb.append("Key resources: ").append(String.join(", ", displayConfigs));
                if (resources.size() > 5) {
                    sb.append(" (+").append(resources.size() - 5).append(" more)");
                }
                sb.append("\n");
            }
        }

        renderTree(root, "", sb, 0, depthLimit);

        return new ArchiveTree(
                archiveName,
                normalizedSubPath,
                totalSize,
                classCount,
                resourceCount,
                new ArrayList<>(packages),
                resources,
                sb.toString()
        );
    }

    private static void renderTree(TreeNode node, String prefix, StringBuilder sb, int depth, int maxDepth) {
        List<TreeNode> sorted = new ArrayList<>(node.children.values());
        sorted.sort((a, b) -> {
            if (a.isLeaf != b.isLeaf) return a.isLeaf ? 1 : -1;
            return a.name.compareToIgnoreCase(b.name);
        });

        int displayCount = sorted.size();
        boolean hasTruncation = sorted.size() > MAX_CHILDREN_PER_DIR;
        if (hasTruncation) {
            displayCount = 12;
        }

        for (int i = 0; i < displayCount; i++) {
            TreeNode child = sorted.get(i);
            boolean isLast = (i == displayCount - 1) && !hasTruncation;
            sb.append(prefix).append(isLast ? LEAF : BRANCH).append(child.name);
            if (child.isLeaf) {
                sb.append(" (").append(formatBytes(child.size)).append(")");
            }
            sb.append("\n");

            if (!child.isLeaf) {
                if (depth + 1 < maxDepth) {
                    renderTree(child, prefix + (isLast ? SPACE : PIPE), sb, depth + 1, maxDepth);
                } else if (!child.children.isEmpty()) {
                    sb.append(prefix).append(isLast ? SPACE : PIPE).append(LEAF).append("... (")
                            .append(child.children.size()).append(" items)\n");
                }
            }
        }

        if (hasTruncation) {
            sb.append(prefix).append(LEAF).append("... and ").append(sorted.size() - displayCount).append(" more items\n");
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.ROOT, "%.1f %cB", bytes / Math.pow(1024, exp), pre);
    }

    private static class TreeNode {
        final String name;
        final boolean isLeaf;
        final long size;
        final Map<String, TreeNode> children = new LinkedHashMap<>();

        TreeNode(String name) {
            this(name, false, 0);
        }

        TreeNode(String name, boolean isLeaf, long size) {
            this.name = name;
            this.isLeaf = isLeaf;
            this.size = size;
        }
    }
}
