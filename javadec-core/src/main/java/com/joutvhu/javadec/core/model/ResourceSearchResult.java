package com.joutvhu.javadec.core.model;

import java.util.List;

/**
 * Result of searching for a text keyword or pattern across all configuration and resource files in an archive.
 */
public record ResourceSearchResult(
        String archiveName,
        String query,
        int totalMatches,
        List<ResourceSearchMatch> matches,
        String formattedSummary
) {
    public static ResourceSearchResult of(String archiveName, String query, List<ResourceSearchMatch> matches) {
        StringBuilder sb = new StringBuilder();
        if (matches.isEmpty()) {
            sb.append("// No matches found for '").append(query).append("' in resource files of ").append(archiveName);
        } else {
            sb.append("Resource Search: '").append(query).append("' in ").append(archiveName);
            sb.append(String.format(" (%d match%s):\n\n", matches.size(), matches.size() == 1 ? "" : "es"));
            for (ResourceSearchMatch match : matches) {
                sb.append("  - ").append(match.toDisplayString()).append("\n");
            }
        }
        return new ResourceSearchResult(archiveName, query, matches.size(), matches, sb.toString().trim());
    }
}
