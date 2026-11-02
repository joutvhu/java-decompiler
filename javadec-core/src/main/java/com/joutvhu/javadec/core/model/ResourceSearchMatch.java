package com.joutvhu.javadec.core.model;

/**
 * Represents a matched line found in a non-class resource file inside an archive.
 */
public record ResourceSearchMatch(
        String resourcePath,
        int lineNumber,
        String lineContent
) {
    public String toDisplayString() {
        return String.format("%s [line %d]: %s", resourcePath, lineNumber, lineContent);
    }
}
