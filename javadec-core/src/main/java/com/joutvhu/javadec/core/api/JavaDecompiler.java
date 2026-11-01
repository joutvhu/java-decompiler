package com.joutvhu.javadec.core.api;

import com.joutvhu.javadec.core.engine.CfrDecompilerEngine;
import com.joutvhu.javadec.core.loader.ZipBytecodeLoader;
import com.joutvhu.javadec.core.model.ArchiveTree;
import com.joutvhu.javadec.core.model.ClassOutline;
import com.joutvhu.javadec.core.outline.AsmClassOutlineExtractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main JavaDecompiler entry point providing outline, selective decompilation,
 * resource inspection, pagination, and archive extraction capabilities.
 */
public class JavaDecompiler {

    private final DecompilerEngine defaultEngine;

    public JavaDecompiler() {
        this.defaultEngine = new CfrDecompilerEngine();
    }

    public JavaDecompiler(DecompilerEngine defaultEngine) {
        this.defaultEngine = defaultEngine;
    }

    /**
     * Lists class entries inside an archive (jar/zip).
     */
    public List<String> listArchiveClasses(File archiveFile, String filterPattern) throws IOException {
        return ZipBytecodeLoader.listClasses(archiveFile, filterPattern);
    }

    /**
     * Inspects archive hierarchy starting from a specific subPath and with a max depth limit.
     */
    public ArchiveTree inspectArchiveTree(File archiveFile, String subPath, Integer maxDepth) throws IOException {
        return ZipBytecodeLoader.inspectTree(archiveFile, subPath, maxDepth);
    }

    public ArchiveTree inspectArchiveTree(File archiveFile) throws IOException {
        return inspectArchiveTree(archiveFile, null, 3);
    }

    /**
     * Reads a text resource file from an archive directly in-memory with optional line pagination.
     */
    public String readArchiveResource(File archiveFile, String resourcePath, Integer lineOffset, Integer lineLimit) throws IOException {
        return ZipBytecodeLoader.readResourceText(archiveFile, resourcePath, lineOffset, lineLimit);
    }

    public String readArchiveResource(File archiveFile, String resourcePath) throws IOException {
        return readArchiveResource(archiveFile, resourcePath, null, null);
    }

    /**
     * Extracts files from an archive to a destination directory on disk.
     */
    public int extractArchive(File archiveFile, File targetDir, String pathFilter) throws IOException {
        return ZipBytecodeLoader.extractArchive(archiveFile, targetDir, pathFilter);
    }

    /**
     * Extracts a token-efficient ClassOutline skeleton directly from bytecode bytes.
     */
    public ClassOutline getOutline(byte[] bytecode) {
        return AsmClassOutlineExtractor.extract(bytecode);
    }

    /**
     * Extracts a ClassOutline from a class inside a jar/zip archive.
     */
    public ClassOutline getArchiveClassOutline(File archiveFile, String classPath) throws IOException {
        byte[] bytes = ZipBytecodeLoader.loadClassBytes(archiveFile, classPath);
        return getOutline(bytes);
    }

    /**
     * Extracts a ClassOutline from a standalone .class file.
     */
    public ClassOutline getClassOutline(File classFile) throws IOException {
        byte[] bytes = Files.readAllBytes(classFile.toPath());
        return getOutline(bytes);
    }

    /**
     * Decompiles complete class source code from bytecode bytes with line pagination support.
     */
    public String decompile(String className, byte[] bytecode, DecompileOptions options) {
        DecompileOptions opts = options != null ? options : DecompileOptions.defaults();
        String fullSource = defaultEngine.decompile(className, bytecode, opts);
        return ZipBytecodeLoader.paginateLines(fullSource, opts.lineOffset(), opts.lineLimit());
    }

    /**
     * Decompiles a class entry directly from a jar/zip archive without disk extraction,
     * with archive context for inner class resolution and line pagination.
     */
    public String decompileArchiveClass(File archiveFile, String classPath, DecompileOptions options) throws IOException {
        byte[] bytes = ZipBytecodeLoader.loadClassBytes(archiveFile, classPath);
        DecompileOptions opts = options != null ? options : DecompileOptions.defaults();
        String fullSource;
        if (defaultEngine instanceof CfrDecompilerEngine cfrEngine) {
            fullSource = cfrEngine.decompileWithContext(classPath, bytes, archiveFile, null, opts);
        } else {
            fullSource = defaultEngine.decompile(classPath, bytes, opts);
        }
        return ZipBytecodeLoader.paginateLines(fullSource, opts.lineOffset(), opts.lineLimit());
    }

    /**
     * Decompiles a standalone .class file with local folder context for inner class resolution.
     */
    public String decompileFile(File classFile, DecompileOptions options) throws IOException {
        byte[] bytes = Files.readAllBytes(classFile.toPath());
        DecompileOptions opts = options != null ? options : DecompileOptions.defaults();
        String fullSource;
        if (defaultEngine instanceof CfrDecompilerEngine cfrEngine) {
            fullSource = cfrEngine.decompileWithContext(classFile.getName(), bytes, null, classFile.getParentFile(), opts);
        } else {
            fullSource = defaultEngine.decompile(classFile.getName(), bytes, opts);
        }
        return ZipBytecodeLoader.paginateLines(fullSource, opts.lineOffset(), opts.lineLimit());
    }

    /**
     * Decompiles and extracts only the specified method body to minimize token usage.
     */
    public String decompileMethod(String className, byte[] bytecode, String methodName, DecompileOptions options) {
        String fullSource = defaultEngine.decompile(className, bytecode, options);
        String extracted = extractMethodFromSource(fullSource, methodName);
        DecompileOptions opts = options != null ? options : DecompileOptions.defaults();
        return ZipBytecodeLoader.paginateLines(extracted, opts.lineOffset(), opts.lineLimit());
    }

    /**
     * Decompiles and extracts only the specified method from an archive class.
     */
    public String decompileArchiveMethod(File archiveFile, String classPath, String methodName, DecompileOptions options) throws IOException {
        byte[] bytes = ZipBytecodeLoader.loadClassBytes(archiveFile, classPath);
        DecompileOptions opts = options != null ? options : DecompileOptions.defaults();
        String fullSource;
        if (defaultEngine instanceof CfrDecompilerEngine cfrEngine) {
            fullSource = cfrEngine.decompileWithContext(classPath, bytes, archiveFile, null, opts);
        } else {
            fullSource = defaultEngine.decompile(classPath, bytes, opts);
        }
        String extracted = extractMethodFromSource(fullSource, methodName);
        return ZipBytecodeLoader.paginateLines(extracted, opts.lineOffset(), opts.lineLimit());
    }

    /**
     * Extracts method declaration(s), preceding annotations, and bodies from decompiled Java source text.
     * Supports overloaded methods, constructors, interface/abstract methods, and annotations.
     */
    public String extractMethodFromSource(String fullSource, String methodName) {
        String cleanMethodName = methodName.equals("<init>") ? "(?:[A-Z]\\w*)" : Pattern.quote(methodName);
        Pattern pattern = Pattern.compile("(?m)^[ \\t]*(?:(?:public|protected|private|static|final|native|synchronized|abstract|default)\\s+)*[\\w<>\\[\\],?\\s]*\\b" + cleanMethodName + "\\s*\\([^)]*\\)[^{;]*(\\{|;)");
        Matcher matcher = pattern.matcher(fullSource);

        List<String> matchedMethods = new ArrayList<>();
        while (matcher.find()) {
            int startIdx = matcher.start();
            // Include preceding annotations/comments on the method
            String prefix = fullSource.substring(0, startIdx);
            String[] lines = prefix.split("\r?\n");
            int backtrackLen = 0;
            for (int i = lines.length - 1; i >= 0; i--) {
                String trimmed = lines[i].trim();
                if (trimmed.startsWith("@") || trimmed.startsWith("/*") || trimmed.startsWith("*") || trimmed.startsWith("//")) {
                    backtrackLen += lines[i].length() + 1;
                } else {
                    break;
                }
            }
            startIdx = Math.max(0, startIdx - backtrackLen);

            String terminator = matcher.group(1);
            if (";".equals(terminator)) {
                matchedMethods.add(fullSource.substring(startIdx, matcher.end()).trim());
            } else {
                int openBraceIdx = matcher.end() - 1;
                int depth = 0;
                int endIdx = -1;
                for (int i = openBraceIdx; i < fullSource.length(); i++) {
                    char c = fullSource.charAt(i);
                    if (c == '{') depth++;
                    else if (c == '}') {
                        depth--;
                        if (depth == 0) {
                            endIdx = i + 1;
                            break;
                        }
                    }
                }
                if (endIdx != -1) {
                    matchedMethods.add(fullSource.substring(startIdx, endIdx).trim());
                } else {
                    matchedMethods.add(fullSource.substring(startIdx).trim());
                }
            }
        }

        if (matchedMethods.isEmpty()) {
            return "// Method '" + methodName + "' not found in decompiled source.";
        }
        return String.join("\n\n", matchedMethods);
    }
}
