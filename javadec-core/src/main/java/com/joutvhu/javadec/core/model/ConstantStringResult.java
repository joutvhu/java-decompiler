package com.joutvhu.javadec.core.model;

import java.util.List;

/**
 * Result of string constants extraction across an archive or class.
 */
public record ConstantStringResult(
        String scopeName,
        int totalFound,
        List<ConstantStringItem> items,
        String formattedSummary
) {
    public static ConstantStringResult of(String scopeName, String filter, List<ConstantStringItem> items) {
        StringBuilder sb = new StringBuilder();
        if (items.isEmpty()) {
            sb.append("// No string constants found");
            if (filter != null && !filter.isBlank()) {
                sb.append(" matching filter '").append(filter).append("'");
            }
            sb.append(" in ").append(scopeName);
        } else {
            sb.append("Found ").append(items.size()).append(" string constant(s) in ").append(scopeName);
            if (filter != null && !filter.isBlank()) {
                sb.append(" (filter: '").append(filter).append("')");
            }
            sb.append(":\n\n");
            for (ConstantStringItem item : items) {
                sb.append("- ").append(item.toDisplayString()).append("\n");
            }
        }
        return new ConstantStringResult(scopeName, items.size(), items, sb.toString());
    }
}
