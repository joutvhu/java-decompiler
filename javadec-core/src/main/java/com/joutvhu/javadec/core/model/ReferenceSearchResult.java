package com.joutvhu.javadec.core.model;

import java.util.List;

/**
 * Result of a bytecode reference search across an archive or directory.
 */
public record ReferenceSearchResult(
        String targetQuery,
        String scopeName,
        int totalFound,
        List<ReferenceItem> references,
        String formattedSummary
) {
    public static ReferenceSearchResult of(String targetQuery, String scopeName, List<ReferenceItem> references) {
        StringBuilder sb = new StringBuilder();
        if (references.isEmpty()) {
            sb.append("// No references found for '").append(targetQuery).append("' in ").append(scopeName);
        } else {
            sb.append("Found ").append(references.size()).append(" reference(s) to '").append(targetQuery).append("' in ").append(scopeName).append(":\n\n");
            for (ReferenceItem ref : references) {
                sb.append("- ").append(ref.toDisplayString()).append("\n");
            }
        }
        return new ReferenceSearchResult(targetQuery, scopeName, references.size(), references, sb.toString());
    }
}
