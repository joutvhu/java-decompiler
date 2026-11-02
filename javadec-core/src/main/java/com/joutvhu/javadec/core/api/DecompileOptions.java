package com.joutvhu.javadec.core.api;

/**
 * Options to configure bytecode decompilation, deobfuscation, formatting, constant relinking, classpath, and source pagination.
 */
public record DecompileOptions(
        boolean showLineNumbers,
        boolean includeSyntheticMembers,
        boolean hideDefaultConstructors,
        boolean deobfuscate,
        boolean showHexLiterals,
        boolean relinkConstants,        // de-inline inlined constants back to field names
        String extraClasspath,          // user-specified auxiliary classpath
        boolean autoDetectDependencies, // whether to auto-detect dependencies from ~/.m2 and pom.xml
        String preferredEngine,         // "auto", "native"
        Integer lineOffset,             // 1-based start line (null for beginning)
        Integer lineLimit               // max lines to return (null for all remaining lines)
) {
    public DecompileOptions(
            boolean showLineNumbers,
            boolean includeSyntheticMembers,
            boolean hideDefaultConstructors,
            boolean deobfuscate,
            boolean showHexLiterals,
            String extraClasspath,
            boolean autoDetectDependencies,
            String preferredEngine,
            Integer lineOffset,
            Integer lineLimit
    ) {
        this(showLineNumbers, includeSyntheticMembers, hideDefaultConstructors, deobfuscate, showHexLiterals,
                false, extraClasspath, autoDetectDependencies, preferredEngine, lineOffset, lineLimit);
    }

    public static DecompileOptions defaults() {
        return new DecompileOptions(false, false, true, false, false, false, null, true, "auto", null, null);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean showLineNumbers = false;
        private boolean includeSyntheticMembers = false;
        private boolean hideDefaultConstructors = true;
        private boolean deobfuscate = false;
        private boolean showHexLiterals = false;
        private boolean relinkConstants = false;
        private String extraClasspath = null;
        private boolean autoDetectDependencies = true;
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

        public Builder deobfuscate(boolean deobfuscate) {
            this.deobfuscate = deobfuscate;
            return this;
        }

        public Builder showHexLiterals(boolean showHexLiterals) {
            this.showHexLiterals = showHexLiterals;
            return this;
        }

        public Builder relinkConstants(boolean relinkConstants) {
            this.relinkConstants = relinkConstants;
            return this;
        }

        public Builder extraClasspath(String extraClasspath) {
            this.extraClasspath = extraClasspath;
            return this;
        }

        public Builder autoDetectDependencies(boolean autoDetectDependencies) {
            this.autoDetectDependencies = autoDetectDependencies;
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
            return new DecompileOptions(showLineNumbers, includeSyntheticMembers, hideDefaultConstructors,
                    deobfuscate, showHexLiterals, relinkConstants, extraClasspath, autoDetectDependencies, preferredEngine, lineOffset, lineLimit);
        }
    }
}
