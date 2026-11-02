package com.joutvhu.javadec.core.model;

/**
 * Represents a hardcoded string literal found in class bytecode or constant pool.
 */
public record ConstantStringItem(
        String className,
        String methodName,
        Integer lineNumber,
        String value
) {
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append(className.replace('/', '.'));
        if (methodName != null && !methodName.isEmpty()) {
            sb.append(".").append(methodName);
        }
        if (lineNumber != null && lineNumber > 0) {
            sb.append(" [line ").append(lineNumber).append("]");
        }
        sb.append(" : \"").append(escapeString(value)).append("\"");
        return sb.toString();
    }

    private static String escapeString(String s) {
        if (s == null) return "";
        return s.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
