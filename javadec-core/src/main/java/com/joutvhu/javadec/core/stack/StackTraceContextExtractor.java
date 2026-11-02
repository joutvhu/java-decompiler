package com.joutvhu.javadec.core.stack;

import com.joutvhu.javadec.core.api.DecompileOptions;
import com.joutvhu.javadec.core.api.JavaDecompiler;
import com.joutvhu.javadec.core.loader.ZipBytecodeLoader;
import com.joutvhu.javadec.core.model.StackTraceSnippetResult;
import org.objectweb.asm.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Maps exception stack trace lines directly to targeted source code snippets.
 * Automatically discovers the containing method from bytecode LineNumberTable,
 * decompiles only that method, and extracts contextual lines surrounding the fault.
 */
public class StackTraceContextExtractor {

    public static StackTraceSnippetResult extract(JavaDecompiler decompiler, File archiveFile, String classPath, int targetLine, Integer contextLines) throws IOException {
        int radius = (contextLines != null && contextLines > 0) ? contextLines : 3;

        // 1. Inspect bytecode via ASM to discover exact containing method for targetLine
        AtomicReference<String> discoveredMethod = new AtomicReference<>(null);
        AtomicReference<String> discoveredClassName = new AtomicReference<>(null);
        try {
            byte[] bytecode = (archiveFile != null)
                    ? ZipBytecodeLoader.loadClassBytes(archiveFile, classPath)
                    : Files.readAllBytes(new File(classPath).toPath());

            if (bytecode != null && bytecode.length >= 8) {
                byte[] compatible = bytecode;
                int majorVersion = ((bytecode[6] & 0xFF) << 8) | (bytecode[7] & 0xFF);
                if (majorVersion > 65) {
                    compatible = bytecode.clone();
                    compatible[6] = 0;
                    compatible[7] = 65;
                }
                ClassReader cr = new ClassReader(compatible);
                cr.accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                        if (name != null) {
                            discoveredClassName.set(name.replace('/', '.'));
                        }
                    }

                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitLineNumber(int line, Label start) {
                                if (line == targetLine) {
                                    discoveredMethod.set(name);
                                }
                            }
                        };
                    }
                }, ClassReader.SKIP_FRAMES);
            }
        } catch (Exception ignored) {}

        // 2. Decompile full class with line numbers to get exact line mappings
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
        String lineTargetPattern3 = "/* Line " + targetLine + " */";

        for (int i = 0; i < lines.length; i++) {
            String l = lines[i];
            if (l.contains(lineTargetPattern1) || l.contains(lineTargetPattern3)
                    || l.matches(".*\\/\\*\\s*[Ll]ine\\s*" + targetLine + "\\s*\\*\\/.*")) {
                matchIdx = i;
                break;
            }
        }

        // If not found via line comment, check if source came from companion -sources.jar (1:1 source lines)
        if (matchIdx < 0 && !decompiled.contains("Decompiled with CFR") && targetLine > 0 && targetLine <= lines.length) {
            matchIdx = targetLine - 1;
        }

        String className = discoveredClassName.get() != null
                ? discoveredClassName.get()
                : classPath.replace('\\', '/').replaceAll("^/+", "").replaceAll("\\.class$", "").replace('/', '.');

        // If still not matched, try matching discovered method declaration
        if (matchIdx < 0 && discoveredMethod.get() != null) {
            String mName = discoveredMethod.get();
            boolean isInit = "<init>".equals(mName);
            boolean isClinit = "<clinit>".equals(mName);
            String simpleName = className.contains(".") ? className.substring(className.lastIndexOf('.') + 1) : className;

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                boolean matched;
                if (isInit) {
                    matched = line.contains(" " + simpleName + "(") || line.contains(" " + simpleName + " (");
                } else if (isClinit) {
                    matched = line.contains("static {") || line.contains("static{");
                } else {
                    matched = line.contains(" " + mName + "(") || line.contains(" " + mName + " (");
                }
                if (matched) {
                    matchIdx = i;
                    break;
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("// Context for ").append(className);
        if (discoveredMethod.get() != null) {
            sb.append(".").append(discoveredMethod.get());
        }
        sb.append(" at line ").append(targetLine).append(":\n\n");

        if (matchIdx >= 0) {
            int start = Math.max(0, matchIdx - radius);
            int end = Math.min(lines.length, matchIdx + radius + 1);
            boolean isFromSources = !decompiled.contains("Decompiled with CFR");

            for (int i = start; i < end; i++) {
                if (i == matchIdx) {
                    sb.append(String.format(" %5d: >>> %s\n", targetLine, lines[i]));
                } else {
                    int lineNum;
                    Integer extracted = extractLineNumberComment(lines[i]);
                    if (extracted != null) {
                        lineNum = extracted;
                    } else if (isFromSources) {
                        lineNum = i + 1;
                    } else {
                        lineNum = targetLine + (i - matchIdx);
                    }
                    if (lineNum > 0) {
                        sb.append(String.format(" %5d:     %s\n", lineNum, lines[i]));
                    } else {
                        sb.append(String.format("      :     %s\n", lines[i]));
                    }
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

    private static Integer extractLineNumberComment(String line) {
        if (line == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("/\\*\\s*[Ll]ine\\s*(\\d+)\\s*\\*/").matcher(line);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
