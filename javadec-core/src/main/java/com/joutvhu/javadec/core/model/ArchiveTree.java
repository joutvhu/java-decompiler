package com.joutvhu.javadec.core.model;

import java.util.*;

/**
 * Represents a hierarchical tree structure of entries in a JAR/ZIP archive.
 * Summarizes packages, classes, and resource files with compact token usage for AI agents.
 */
public record ArchiveTree(
        String archiveName,
        long totalSize,
        int totalClasses,
        int totalResources,
        List<String> topLevelPackages,
        List<String> keyResources,
        String formattedTree
) {

    public static ArchiveTree build(String archiveName, long totalSize, Map<String, Long> entries) {
        int classCount = 0;
        int resourceCount = 0;
        Set<String> packages = new TreeSet<>();
        List<String> resources = new ArrayList<>();

        TreeNode root = new TreeNode("");

        for (Map.Entry<String, Long> entry : entries.entrySet()) {
            String path = entry.getKey();
            if (path.endsWith("/")) continue;

            if (path.endsWith(".class")) {
                classCount++;
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash > 0) {
                    String pkg = path.substring(0, lastSlash).replace('/', '.');
                    int dotIdx = pkg.indexOf('.');
                    packages.add(dotIdx > 0 ? pkg.substring(0, pkg.indexOf('.', dotIdx + 1) > 0 ? pkg.indexOf('.', dotIdx + 1) : pkg.length()) : pkg);
                }
            } else {
                resourceCount++;
                if (path.startsWith("META-INF/") || path.endsWith(".yml") || path.endsWith(".yaml")
                        || path.endsWith(".properties") || path.endsWith(".xml") || path.endsWith(".json")
                        || path.endsWith(".sql") || path.endsWith(".proto")) {
                    resources.add(path);
                }
            }

            // Build hierarchical tree
            String[] segments = path.split("/");
            TreeNode current = root;
            for (int i = 0; i < segments.length; i++) {
                String segment = segments[i];
                boolean isLeaf = (i == segments.length - 1);
                current = current.children.computeIfAbsent(segment, s -> new TreeNode(s, isLeaf, isLeaf ? entry.getValue() : 0));
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(archiveName).append(" (").append(formatBytes(totalSize)).append(", ")
                .append(classCount).append(" classes, ")
                .append(resourceCount).append(" resources)\n");
        renderTree(root, "", sb, 0, 3); // Max depth 3 for concise overview

        return new ArchiveTree(
                archiveName,
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

        for (int i = 0; i < sorted.size(); i++) {
            TreeNode child = sorted.get(i);
            boolean isLast = (i == sorted.size() - 1);
            sb.append(prefix).append(isLast ? "└── " : "├── ").append(child.name);
            if (child.isLeaf) {
                sb.append(" (").append(formatBytes(child.size)).append(")");
            }
            sb.append("\n");

            if (!child.isLeaf) {
                if (depth < maxDepth) {
                    renderTree(child, prefix + (isLast ? "    " : "│   "), sb, depth + 1, maxDepth);
                } else if (!child.children.isEmpty()) {
                    sb.append(prefix).append(isLast ? "    " : "│   ").append("└── ... (")
                            .append(child.children.size()).append(" items)\n");
                }
            }
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
