package com.joutvhu.javadec.core.api;

/**
 * Options to configure bytecode decompilation.
 */
public record DecompileOptions(
        boolean showLineNumbers,
        boolean includeSyntheticMembers,
        boolean hideDefaultConstructors,
        String preferredEngine // "fernflower", "cfr", or "auto"
) {
    public static DecompileOptions defaults() {
        return new DecompileOptions(false, false, true, "auto");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean showLineNumbers = false;
        private boolean includeSyntheticMembers = false;
        private boolean hideDefaultConstructors = true;
        private String preferredEngine = "auto";

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

        public DecompileOptions build() {
            return new DecompileOptions(showLineNumbers, includeSyntheticMembers, hideDefaultConstructors, preferredEngine);
        }
    }
}
