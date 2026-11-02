package com.joutvhu.javadec.core.model;

import java.util.List;

/**
 * Result of JVM bytecode technical metrics analysis across a class.
 */
public record ClassMetricsResult(
        String className,
        int methodCount,
        int fieldCount,
        List<MethodMetricsItem> methods,
        String formattedSummary
) {
    public static ClassMetricsResult of(String className, int fieldCount, List<MethodMetricsItem> methods) {
        StringBuilder sb = new StringBuilder();
        sb.append("JVM Bytecode Metrics for ").append(className.replace('/', '.')).append(":\n");
        sb.append(String.format("Summary: %d methods, %d fields\n\n", methods.size(), fieldCount));

        if (methods.isEmpty()) {
            sb.append("// No methods found.\n");
        } else {
            for (MethodMetricsItem m : methods) {
                sb.append("  - ").append(m.toDisplayString()).append("\n");
            }
        }

        return new ClassMetricsResult(className, methods.size(), fieldCount, methods, sb.toString().trim());
    }
}
