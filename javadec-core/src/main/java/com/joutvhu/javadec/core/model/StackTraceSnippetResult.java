package com.joutvhu.javadec.core.model;

/**
 * Result of inspecting a specific line number from a stack trace or exception log.
 */
public record StackTraceSnippetResult(
        String className,
        String methodName,
        int targetLineNumber,
        String snippet
) {}
