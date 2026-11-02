package com.joutvhu.javadec.core.model;

/**
 * Represents an outgoing method invocation (callee) called from inside a method.
 */
public record CalleeItem(
        String callerMethod,
        String targetOwner,
        String targetName,
        String targetDesc,
        Integer lineNumber,
        boolean isDynamic
) {
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append(targetOwner.replace('/', '.')).append(".").append(targetName);
        if (targetDesc != null && !targetDesc.isEmpty()) {
            sb.append(targetDesc);
        }
        if (lineNumber != null && lineNumber > 0) {
            sb.append(" [line ").append(lineNumber).append("]");
        }
        if (isDynamic) {
            sb.append(" (dynamic/ref)");
        }
        return sb.toString();
    }
}
