package com.joutvhu.javadec.core.model;

/**
 * Represents a security vulnerability or risky anti-pattern detected in bytecode.
 */
public record SecurityFindingItem(
        String ruleId,
        Severity severity,
        String className,
        String methodName,
        Integer lineNumber,
        String description,
        String evidence
) {
    public enum Severity {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }

    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(severity).append("] ");
        sb.append(ruleId).append(": ").append(description).append("\n");
        sb.append("      Location: ").append(className.replace('/', '.'));
        if (methodName != null && !methodName.isEmpty()) {
            sb.append(".").append(methodName);
        }
        if (lineNumber != null && lineNumber > 0) {
            sb.append(" [line ").append(lineNumber).append("]");
        }
        sb.append("\n");
        if (evidence != null && !evidence.isEmpty()) {
            sb.append("      Evidence: ").append(evidence).append("\n");
        }
        return sb.toString();
    }
}
