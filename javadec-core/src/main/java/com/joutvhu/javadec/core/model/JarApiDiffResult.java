package com.joutvhu.javadec.core.model;

import java.util.List;

/**
 * Result of comparing binary public API differences between two JAR archives or library versions.
 */
public record JarApiDiffResult(
        String scope1,
        String scope2,
        int addedCount,
        int removedCount,
        int modifiedCount,
        List<DiffItem> differences,
        String formattedReport
) {
    public enum DiffType {
        CLASS_ADDED,
        CLASS_REMOVED,
        METHOD_ADDED,
        METHOD_REMOVED,
        METHOD_MODIFIED,
        FIELD_ADDED,
        FIELD_REMOVED,
        FIELD_MODIFIED
    }

    public record DiffItem(
            DiffType type,
            String targetElement,
            String details,
            boolean isBreaking
    ) {
        public String toDisplayString() {
            String tag = switch (type) {
                case CLASS_ADDED, METHOD_ADDED, FIELD_ADDED -> "[ADDED]   ";
                case CLASS_REMOVED, METHOD_REMOVED, FIELD_REMOVED -> "[REMOVED] ";
                case METHOD_MODIFIED, FIELD_MODIFIED -> "[CHANGED] ";
            };
            return tag + targetElement + (details != null && !details.isEmpty() ? " (" + details + ")" : "");
        }
    }
}
