package com.joutvhu.javadec.core.api;

import com.joutvhu.javadec.core.engine.CfrDecompilerEngine;
import com.joutvhu.javadec.core.loader.ZipBytecodeLoader;
import com.joutvhu.javadec.core.model.ArchiveTree;
import com.joutvhu.javadec.core.model.ClassOutline;
import com.joutvhu.javadec.core.model.ExportResult;
import com.joutvhu.javadec.core.outline.AsmClassOutlineExtractor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Main JavaDecompiler entry point providing outline, selective decompilation,
 * companion sources jar resolution (Javadoc/comment preservation), resource inspection,
 * pagination, archive extraction, and full source export capabilities.
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
     * Extracts raw files from an archive to a destination directory on disk.
     */
    public int extractArchive(File archiveFile, File targetDir, String pathFilter) throws IOException {
        return ZipBytecodeLoader.extractArchive(archiveFile, targetDir, pathFilter);
    }

    /**
     * Decompiles all (or filtered) classes in an archive into .java files on disk,
     * maintaining package folder structures and copying non-class resources.
     */
    public ExportResult exportArchive(File archiveFile, File outputDir, String pathFilter, DecompileOptions options) throws IOException {
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        DecompileOptions opts = options != null ? options : DecompileOptions.defaults();
        String slashFilter = pathFilter != null ? pathFilter.trim().replace('\\', '/').replaceAll("^/+", "").replace('.', '/') : null;

        int decompiledCount = 0;
        int resourceCount = 0;
        int failCount = 0;

        try (ZipFile zipFile = new ZipFile(archiveFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory()) continue;

                // Strip Spring Boot / WAR prefixes if present
                String cleanName = name;
                if (cleanName.startsWith("BOOT-INF/classes/")) {
                    cleanName = cleanName.substring("BOOT-INF/classes/".length());
                } else if (cleanName.startsWith("WEB-INF/classes/")) {
                    cleanName = cleanName.substring("WEB-INF/classes/".length());
                }

                if (slashFilter != null && !cleanName.startsWith(slashFilter) && !cleanName.contains(slashFilter)) {
                    continue;
                }

                if (cleanName.endsWith(".class")) {
                    // Skip anonymous or inner classes during top-level loop; CFR will inline them into outer class
                    int dollarIdx = cleanName.lastIndexOf('$');
                    if (dollarIdx > 0 && dollarIdx > cleanName.lastIndexOf('/')) {
                        continue;
                    }

                    try {
                        String source = decompileArchiveClass(archiveFile, cleanName, opts);
                        String javaFileName = cleanName.substring(0, cleanName.length() - 6) + ".java";
                        File destJavaFile = new File(outputDir, javaFileName);

                        // Validate against Zip Slip
                        Path destPath = destJavaFile.toPath().normalize();
                        if (!destPath.startsWith(outputDir.toPath().normalize())) {
                            throw new IOException("Bad zip entry path: " + name);
                        }

                        destJavaFile.getParentFile().mkdirs();
                        Files.writeString(destPath, source, StandardCharsets.UTF_8);
                        decompiledCount++;
                    } catch (Exception e) {
                        failCount++;
                    }
                } else {
                    // Copy resource file to output directory
                    File destResourceFile = new File(outputDir, cleanName);
                    Path destPath = destResourceFile.toPath().normalize();
                    if (!destPath.startsWith(outputDir.toPath().normalize())) {
                        throw new IOException("Bad zip entry path: " + name);
                    }

                    destResourceFile.getParentFile().mkdirs();
                    try (InputStream is = zipFile.getInputStream(entry);
                         OutputStream os = Files.newOutputStream(destPath)) {
                        is.transferTo(os);
                    }
                    resourceCount++;
                }
            }
        }

        return new ExportResult(decompiledCount, resourceCount, failCount, outputDir.getAbsolutePath());
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
     * Decompiles or resolves original Java source code with Javadoc.
     * Automatically inspects companion `-sources.jar` if available; otherwise falls back to native decompilation.
     */
    public String decompileArchiveClass(File archiveFile, String classPath, DecompileOptions options) throws IOException {
        DecompileOptions opts = options != null ? options : DecompileOptions.defaults();

        // 1. Check for companion -sources.jar to retrieve original Javadoc and comments
        File sourcesJar = ZipBytecodeLoader.findCompanionSourcesJar(archiveFile);
        if (sourcesJar != null) {
            String originalSource = ZipBytecodeLoader.readSourceFromSourcesJar(sourcesJar, classPath);
            if (originalSource != null && !originalSource.isBlank()) {
                return ZipBytecodeLoader.paginateLines(originalSource, opts.lineOffset(), opts.lineLimit());
            }
        }

        // 2. Fall back to native decompiler
        byte[] bytes = ZipBytecodeLoader.loadClassBytes(archiveFile, classPath);
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
     * Decompiles and extracts only the specified method from an archive class,
     * preserving original Javadoc and comments if a companion `-sources.jar` is present.
     */
    public String decompileArchiveMethod(File archiveFile, String classPath, String methodName, DecompileOptions options) throws IOException {
        String fullSource = decompileArchiveClass(archiveFile, classPath, options);
        String extracted = extractMethodFromSource(fullSource, methodName);
        DecompileOptions opts = options != null ? options : DecompileOptions.defaults();
        return ZipBytecodeLoader.paginateLines(extracted, opts.lineOffset(), opts.lineLimit());
    }

    /**
     * Extracts method declaration(s), preceding annotations, and bodies from decompiled Java source text.
     * Supports overloaded methods, constructors, interface/abstract methods, annotations, and CRLF line endings.
     */
    public String extractMethodFromSource(String fullSource, String methodName) {
        String rawMethod = methodName.contains("(") ? methodName.substring(0, methodName.indexOf('(')).trim() : methodName.trim();
        String cleanMethodName = rawMethod.equals("<init>") ? "(?:[A-Z]\\w*)" : Pattern.quote(rawMethod);
        Pattern pattern = Pattern.compile("(?m)^[ \\t]*(?:(?:public|protected|private|static|final|native|synchronized|abstract|default)\\s+)*[\\w<>\\[\\],?\\s]*\\b" + cleanMethodName + "\\s*\\([^)]*\\)[^{;]*(\\{|;)");
        Matcher matcher = pattern.matcher(fullSource);

        List<String> matchedMethods = new ArrayList<>();
        while (matcher.find()) {
            int startIdx = matcher.start();

            // Precise CRLF/LF backward scan for preceding annotations and Javadocs
            int currentPos = startIdx;
            while (currentPos > 0) {
                int prevLineEnd = currentPos;
                if (prevLineEnd > 0 && fullSource.charAt(prevLineEnd - 1) == '\n') prevLineEnd--;
                if (prevLineEnd > 0 && fullSource.charAt(prevLineEnd - 1) == '\r') prevLineEnd--;

                int prevLineStart = prevLineEnd;
                while (prevLineStart > 0 && fullSource.charAt(prevLineStart - 1) != '\n' && fullSource.charAt(prevLineStart - 1) != '\r') {
                    prevLineStart--;
                }

                String prevLineTrimmed = fullSource.substring(prevLineStart, prevLineEnd).trim();
                if (prevLineTrimmed.startsWith("@") || prevLineTrimmed.startsWith("/*")
                        || prevLineTrimmed.startsWith("*") || prevLineTrimmed.startsWith("//")) {
                    currentPos = prevLineStart;
                } else {
                    break;
                }
            }
            startIdx = currentPos;

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
