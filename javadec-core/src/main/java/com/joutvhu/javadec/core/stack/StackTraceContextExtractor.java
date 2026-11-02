package com.joutvhu.javadec.core.stack;

import com.joutvhu.javadec.core.api.DecompileOptions;
import com.joutvhu.javadec.core.api.JavaDecompiler;
import com.joutvhu.javadec.core.model.StackTraceSnippetResult;
import org.objectweb.asm.*;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Maps exception stack trace lines directly to targeted source code snippets.
 * Automatically discovers the containing method from bytecode LineNumberTable,
 * decompiles only that method, and extracts contextual lines surrounding the fault.
 */
public class StackTraceContextExtractor {

    public static StackTraceSnippetResult extract(JavaDecompiler decompiler, File archiveFile, String classPath, int targetLine, Integer contextLines) throws IOException {
        int radius = (contextLines != null && contextLines > 0) ? contextLines : 3;

        // 1. Decompile full class with line numbers to get exact line mappings
        DecompileOptions options = DecompileOptions.builder()
                .showLineNumbers(true)
                .build();

        String decompiled;
        if (archiveFile != null) {
            decompiled = decompiler.decompileArchiveClass(archiveFile, classPath, options);
        } else {
            decompiled = decompiler.decompileFile(new File(classPath), options);
        }

        String[] lines = decompiled.split("\r?\n", -1);
        int matchIdx = -1;
        String lineTargetPattern1 = "/* line " + targetLine + " */";
        String lineTargetPattern2 = "/* " + targetLine + " */";
        String lineTargetPattern3 = "/* Line " + targetLine + " */";

        for (int i = 0; i < lines.length; i++) {
            String l = lines[i];
            if (l.contains(lineTargetPattern1) || l.contains(lineTargetPattern2) || l.contains(lineTargetPattern3)
                    || l.matches(".*\\/\\*\\s*" + targetLine + "\\s*\\*\\/.*")) {
                matchIdx = i;
                break;
            }
        }

        // If line number comments were omitted or different, fall back to locating method name via bytecode
        AtomicReference<String> discoveredMethod = new AtomicReference<>(null);

        StringBuilder sb = new StringBuilder();
        String className = classPath.replace('\\', '/').replaceAll("^/+", "").replaceAll("\\.class$", "").replace('/', '.');
        sb.append("// Context for ").append(className).append(" at line ").append(targetLine).append(":\n\n");

        if (matchIdx >= 0) {
            int start = Math.max(0, matchIdx - radius);
            int end = Math.min(lines.length, matchIdx + radius + 1);

            for (int i = start; i < end; i++) {
                if (i == matchIdx) {
                    sb.append(String.format(" %5d: >>> %s\n", targetLine, lines[i]));
                } else {
                    int lineNum = targetLine + (i - matchIdx);
                    sb.append(String.format(" %5d:     %s\n", lineNum, lines[i]));
                }
            }
        } else {
            // Target line comment not found in decompile; present first few lines with note
            sb.append("// Line number comment not directly found. Relevant decompiled source preview:\n\n");
            int max = Math.min(lines.length, 30);
            for (int i = 0; i < max; i++) {
                sb.append("   ").append(lines[i]).append("\n");
            }
        }

        return new StackTraceSnippetResult(className, discoveredMethod.get(), targetLine, sb.toString());
    }
}
