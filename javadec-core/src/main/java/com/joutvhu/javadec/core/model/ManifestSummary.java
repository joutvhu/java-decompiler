package com.joutvhu.javadec.core.model;

import java.util.List;
import java.util.Map;

/**
 * Structured summary of an archive's MANIFEST.MF and packaging metadata.
 */
public record ManifestSummary(
        String archiveName,
        String packagingType,
        String mainClass,
        String startClass,
        String buildJdk,
        String createdBy,
        boolean isMultiRelease,
        Map<String, String> keyAttributes,
        Map<String, List<String>> spiServices,
        String formattedSummary
) {}
