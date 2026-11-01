package com.joutvhu.javadec.core.model;

/**
 * Summary result of exporting/decompiling an archive to disk.
 */
public record ExportResult(
        int decompiledClasses,
        int copiedResources,
        int failedClasses,
        String outputDirectory
) {
    public String toSummaryString() {
        return String.format(
                "Export completed: %d class(es) decompiled to .java, %d resource(s) copied, %d failed into %s",
                decompiledClasses, copiedResources, failedClasses, outputDirectory
        );
    }
}
