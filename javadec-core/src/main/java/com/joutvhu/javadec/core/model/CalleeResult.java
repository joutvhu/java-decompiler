package com.joutvhu.javadec.core.model;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Result of analyzing outgoing method calls (callees) from a class or specific method.
 */
public record CalleeResult(
        String className,
        String filterMethod,
        int totalFound,
        List<CalleeItem> callees,
        String formattedSummary
) {
    public static CalleeResult of(String className, String filterMethod, List<CalleeItem> items) {
        StringBuilder sb = new StringBuilder();
        String displayClass = className.replace('/', '.');

        if (items.isEmpty()) {
            sb.append("// No outgoing method calls found in ").append(displayClass);
            if (filterMethod != null && !filterMethod.isBlank()) {
                sb.append(".").append(filterMethod);
            }
        } else {
            sb.append("Outgoing Method Calls (Callees) for ").append(displayClass);
            if (filterMethod != null && !filterMethod.isBlank()) {
                sb.append(".").append(filterMethod);
            }
            sb.append(" (Total: ").append(items.size()).append("):\n\n");

            // Group by caller method
            Map<String, List<CalleeItem>> byCaller = new TreeMap<>();
            for (CalleeItem item : items) {
                byCaller.computeIfAbsent(item.callerMethod(), k -> new java.util.ArrayList<>()).add(item);
            }

            for (Map.Entry<String, List<CalleeItem>> entry : byCaller.entrySet()) {
                sb.append(displayClass).append(".").append(entry.getKey()).append(":\n");
                for (CalleeItem callee : entry.getValue()) {
                    sb.append("  |-- ").append(callee.toDisplayString()).append("\n");
                }
                sb.append("\n");
            }
        }

        return new CalleeResult(className, filterMethod, items.size(), items, sb.toString().trim());
    }
}
