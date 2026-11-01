package com.joutvhu.javadec.core.model;

/**
 * Represents a single reference (usage) of a class, method, or field in bytecode.
 */
public record ReferenceItem(
        String sourceClass,
        String sourceMethod,
        String sourceMethodDesc,
        Integer lineNumber,
        ReferenceKind kind,
        String targetDetails
) {
    public enum ReferenceKind {
        METHOD_CALL,
        FIELD_READ,
        FIELD_WRITE,
        INSTANTIATION,
        EXTENDS,
        IMPLEMENTS,
        TYPE_CHECK,     // instanceof or checkcast
        TYPE_USAGE      // field/param/return type or annotation
    }

    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append(sourceClass.replace('/', '.'));
        if (sourceMethod != null && !sourceMethod.isEmpty()) {
            sb.append(".").append(sourceMethod);
        }
        if (lineNumber != null && lineNumber > 0) {
            sb.append(" [line ").append(lineNumber).append("]");
        }
        sb.append(" -> ").append(kind);
        if (targetDetails != null && !targetDetails.isEmpty()) {
            sb.append(" (").append(targetDetails).append(")");
        }
        return sb.toString();
    }
}
