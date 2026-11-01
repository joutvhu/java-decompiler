package com.joutvhu.javadec.core.api;

/**
 * Options to configure bytecode decompilation and source pagination.
 */
public record DecompileOptions(
        boolean showLineNumbers,
        boolean includeSyntheticMembers,
        boolean hideDefaultConstructors,
        String preferredEngine, // "fernflower", "cfr", or "auto"
        Integer lineOffset,     // 1-based start line (null for beginning)
        Integer lineLimit       // max lines to return (null for all remaining lines)
) {
    public static DecompileOptions defaults() {
        return new DecompileOptions(false, false, true, "auto", null, null);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean showLineNumbers = false;
        private boolean includeSyntheticMembers = false;
        private boolean hideDefaultConstructors = true;
        private String preferredEngine = "auto";
        private Integer lineOffset = null;
        private Integer lineLimit = null;

        public Builder showLineNumbers(boolean showLineNumbers) {
            this.showLineNumbers = showLineNumbers;
            return this;
        }

        public Builder includeSyntheticMembers(boolean includeSyntheticMembers) {
            this.includeSyntheticMembers = includeSyntheticMembers;
            return this;
        }

        public Builder hideDefaultConstructors(boolean hideDefaultConstructors) {
            this.hideDefaultConstructors = hideDefaultConstructors;
            return this;
        }

        public Builder preferredEngine(String preferredEngine) {
            this.preferredEngine = preferredEngine;
            return this;
        }

        public Builder lineOffset(Integer lineOffset) {
            this.lineOffset = lineOffset;
            return this;
        }

        public Builder lineLimit(Integer lineLimit) {
            this.lineLimit = lineLimit;
            return this;
        }

        public DecompileOptions build() {
            return new DecompileOptions(showLineNumbers, includeSyntheticMembers, hideDefaultConstructors, preferredEngine, lineOffset, lineLimit);
        }
    }
}
