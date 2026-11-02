package com.joutvhu.javadec.core.model;

import com.joutvhu.javadec.core.model.SecurityFindingItem.Severity;

import java.util.List;

/**
 * Result of a bytecode security and risk posture audit across an archive or directory.
 */
public record SecurityAuditResult(
        String scopeName,
        int criticalCount,
        int highCount,
        int mediumCount,
        int lowCount,
        List<SecurityFindingItem> findings,
        String formattedSummary
) {
    public static SecurityAuditResult of(String scopeName, List<SecurityFindingItem> findings) {
        int crit = 0, high = 0, med = 0, low = 0;
        for (SecurityFindingItem item : findings) {
            if (item.severity() == Severity.CRITICAL) crit++;
            else if (item.severity() == Severity.HIGH) high++;
            else if (item.severity() == Severity.MEDIUM) med++;
            else low++;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Bytecode Security Audit Report: ").append(scopeName).append("\n");
        sb.append(String.format("Findings Summary: %d Critical, %d High, %d Medium, %d Low (Total: %d)\n\n",
                crit, high, med, low, findings.size()));

        if (findings.isEmpty()) {
            sb.append("// No known security anti-patterns or critical risks detected in bytecode.\n");
        } else {
            for (SecurityFindingItem finding : findings) {
                sb.append(finding.toDisplayString()).append("\n");
            }
        }

        return new SecurityAuditResult(scopeName, crit, high, med, low, findings, sb.toString().trim());
    }
}
