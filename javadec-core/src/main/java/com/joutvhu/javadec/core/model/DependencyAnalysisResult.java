package com.joutvhu.javadec.core.model;

import java.util.Set;
import java.util.TreeSet;

/**
 * Result of analyzing all class and package dependencies referenced by a compiled Java class.
 */
public record DependencyAnalysisResult(
        String className,
        Set<String> jdkDependencies,
        Set<String> internalDependencies,
        Set<String> thirdPartyDependencies,
        String formattedReport
) {
    public static DependencyAnalysisResult of(String className, Set<String> jdk, Set<String> internal, Set<String> thirdParty) {
        StringBuilder sb = new StringBuilder();
        String displayClass = className.replace('/', '.');
        sb.append("Dependencies for ").append(displayClass).append(":\n\n");

        if (!internal.isEmpty()) {
            sb.append("Internal (Same project/package):\n");
            for (String dep : new TreeSet<>(internal)) {
                sb.append("  - ").append(dep).append("\n");
            }
            sb.append("\n");
        }

        if (!thirdParty.isEmpty()) {
            sb.append("Third-Party Libraries:\n");
            for (String dep : new TreeSet<>(thirdParty)) {
                sb.append("  - ").append(dep).append("\n");
            }
            sb.append("\n");
        }

        if (!jdk.isEmpty()) {
            sb.append("JDK Standard:\n");
            for (String dep : new TreeSet<>(jdk)) {
                sb.append("  - ").append(dep).append("\n");
            }
        }

        return new DependencyAnalysisResult(className, jdk, internal, thirdParty, sb.toString().trim());
    }
}
