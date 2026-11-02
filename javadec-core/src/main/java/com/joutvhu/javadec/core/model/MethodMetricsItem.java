package com.joutvhu.javadec.core.model;

/**
 * Technical JVM metrics for a compiled method.
 */
public record MethodMetricsItem(
        String methodName,
        String descriptor,
        int instructionCount,
        int maxStack,
        int maxLocals,
        int tryCatchCount,
        boolean isLargeMethod
) {
    public String toDisplayString() {
        String warning = isLargeMethod ? " [WARN: Large Method!]" : "";
        return String.format("%s%s -> %d instructions, maxStack=%d, maxLocals=%d, %d try-catch%s",
                methodName, descriptor, instructionCount, maxStack, maxLocals, tryCatchCount, warning);
    }
}
