package com.joutvhu.javadec.core.api;

import com.joutvhu.javadec.core.engine.CfrDecompilerEngine;
import com.joutvhu.javadec.core.loader.ZipBytecodeLoader;
import com.joutvhu.javadec.core.model.ArchiveTree;
import com.joutvhu.javadec.core.model.ClassOutline;
import com.joutvhu.javadec.core.model.ExportResult;
import com.joutvhu.javadec.core.model.ReferenceSearchResult;
import com.joutvhu.javadec.core.outline.AsmClassOutlineExtractor;
import com.joutvhu.javadec.core.scanner.BytecodeReferenceScanner;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
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
     * Scans an archive for references/usages of a class, method, or field.
     */
    public ReferenceSearchResult findReferences(File archiveFile, String query, Integer limit, String pathFilter) throws IOException {
        return BytecodeReferenceScanner.scanArchive(archiveFile, query, limit, pathFilter);
    }

    public ReferenceSearchResult findReferences(File archiveFile, String query) throws IOException {
        return findReferences(archiveFile, query, 100, null);
    }

    /**
     * Scans a directory of compiled classes for references/usages of a class, method, or field.
     */
    public ReferenceSearchResult findReferencesInDirectory(File directory, String query, Integer limit) throws IOException {
        return BytecodeReferenceScanner.scanDirectory(directory, query, limit);
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
                    // Skip anonymous or member inner classes during top-level loop; CFR will inline them into outer class
                    int lastSlash = cleanName.lastIndexOf('/');
                    int dollarIdx = cleanName.lastIndexOf('$');
                    if (dollarIdx > (lastSlash + 1)) {
                        continue;
                    }

                    try {
                        String source = decompileArchiveClass(archiveFile, cleanName, opts);
                        String javaFileName = cleanName.substring(0, cleanName.length() - 6) + ".java";
                        File destJavaFile = new File(outputDir, javaFileName);

                        // Validate against Zip Slip
                        Path destPath = destJavaFile.toPath().toAbsolutePath().normalize();
                        Path baseOutputDir = outputDir.toPath().toAbsolutePath().normalize();
                        if (!destPath.startsWith(baseOutputDir)) {
                            throw new IOException("Bad zip entry path: " + name);
                        }

                        destJavaFile.getParentFile().mkdirs();
                        Files.writeString(destPath, source, StandardCharsets.UTF_8);
                        decompiledCount++;
                    } catch (Exception e) {
                        failCount++;
                    }
                } else {
                    // Skip embedded fat-jar libraries from source export
                    if (cleanName.endsWith(".jar")) {
                        continue;
                    }

                    // Copy resource file to output directory
                    File destResourceFile = new File(outputDir, cleanName);
                    Path destPath = destResourceFile.toPath().toAbsolutePath().normalize();
                    Path baseOutputDir = outputDir.toPath().toAbsolutePath().normalize();
                    if (!destPath.startsWith(baseOutputDir)) {
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
        if (classFile.getName().endsWith(".java")) {
            throw new IllegalArgumentException("'" + classFile.getName() + "' is a Java source file, not a compiled .class file. Use 'javadec view' to view source files.");
        }
        byte[] bytes = Files.readAllBytes(classFile.toPath());
        return getOutline(bytes);
    }

    /**
     * Decompiles complete class source code from bytecode bytes with line pagination support.
     */
    public String decompile(String className, byte[] bytecode, DecompileOptions options) {
        return decompile(className, bytecode, null, options);
    }

    /**
     * Decompiles complete class source code with in-memory sibling/inner classes map.
     */
    public String decompile(String className, byte[] bytecode, Map<String, byte[]> additionalClasses, DecompileOptions options) {
        DecompileOptions opts = options != null ? options : DecompileOptions.defaults();
        String fullSource;
        if (defaultEngine instanceof CfrDecompilerEngine cfrEngine) {
            fullSource = cfrEngine.decompileWithContext(className, bytecode, null, null, additionalClasses, opts);
        } else {
            fullSource = defaultEngine.decompile(className, bytecode, opts);
        }
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
     * Decompiles a standalone .class file (or reads .java file directly) with local folder context for inner class resolution.
     */
    public String decompileFile(File classFile, DecompileOptions options) throws IOException {
        DecompileOptions opts = options != null ? options : DecompileOptions.defaults();
        if (classFile.getName().endsWith(".java") && classFile.exists()) {
            String src = Files.readString(classFile.toPath(), StandardCharsets.UTF_8);
            return ZipBytecodeLoader.paginateLines(src, opts.lineOffset(), opts.lineLimit());
        }

        byte[] bytes = Files.readAllBytes(classFile.toPath());
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
        DecompileOptions opts = options != null ? options : DecompileOptions.defaults();
        DecompileOptions unpagedOpts = DecompileOptions.builder()
                .showLineNumbers(opts.showLineNumbers())
                .includeSyntheticMembers(opts.includeSyntheticMembers())
                .hideDefaultConstructors(opts.hideDefaultConstructors())
                .preferredEngine(opts.preferredEngine())
                .build();
        String fullSource = defaultEngine.decompile(className, bytecode, unpagedOpts);
        String extracted = extractMethodFromSource(fullSource, methodName);
        return ZipBytecodeLoader.paginateLines(extracted, opts.lineOffset(), opts.lineLimit());
    }

    /**
     * Decompiles and extracts only the specified method from a standalone file on disk,
     * maintaining local folder context for inner class resolution.
     */
    public String decompileFileMethod(File classFile, String methodName, DecompileOptions options) throws IOException {
        DecompileOptions opts = options != null ? options : DecompileOptions.defaults();
        DecompileOptions unpagedOpts = DecompileOptions.builder()
                .showLineNumbers(opts.showLineNumbers())
                .includeSyntheticMembers(opts.includeSyntheticMembers())
                .hideDefaultConstructors(opts.hideDefaultConstructors())
                .preferredEngine(opts.preferredEngine())
                .build();
        String fullSource = decompileFile(classFile, unpagedOpts);
        String extracted = extractMethodFromSource(fullSource, methodName);
        return ZipBytecodeLoader.paginateLines(extracted, opts.lineOffset(), opts.lineLimit());
    }

    /**
     * Decompiles and extracts only the specified method from an archive class,
     * preserving original Javadoc and comments if a companion `-sources.jar` is present.
     */
    public String decompileArchiveMethod(File archiveFile, String classPath, String methodName, DecompileOptions options) throws IOException {
        DecompileOptions opts = options != null ? options : DecompileOptions.defaults();
        DecompileOptions unpagedOpts = DecompileOptions.builder()
                .showLineNumbers(opts.showLineNumbers())
                .includeSyntheticMembers(opts.includeSyntheticMembers())
                .hideDefaultConstructors(opts.hideDefaultConstructors())
                .preferredEngine(opts.preferredEngine())
                .build();
        String fullSource = decompileArchiveClass(archiveFile, classPath, unpagedOpts);
        String extracted = extractMethodFromSource(fullSource, methodName);
        return ZipBytecodeLoader.paginateLines(extracted, opts.lineOffset(), opts.lineLimit());
    }

    /**
     * Returns true if the character at position pos is escaped by an odd number of preceding backslashes.
     */
    private static boolean isEscaped(String s, int pos) {
        int backslashCount = 0;
        for (int j = pos - 1; j >= 0 && s.charAt(j) == '\\'; j--) {
            backslashCount++;
        }
        return (backslashCount % 2) != 0;
    }

    /**
     * Extracts method declaration(s), preceding annotations, and bodies from decompiled Java source text.
     * Accurately parses nested braces while ignoring braces inside string literals, characters, and comments.
     */
    public String extractMethodFromSource(String fullSource, String methodName) {
        String rawMethod = methodName.trim();
        boolean hasParamHint = rawMethod.contains("(") && rawMethod.contains(")");
        if (hasParamHint) {
            rawMethod = rawMethod.substring(0, rawMethod.indexOf('(')).trim();
        }

        boolean isStaticInit = rawMethod.equals("<clinit>") || rawMethod.equalsIgnoreCase("static");
        Pattern pattern;
        if (isStaticInit) {
            pattern = Pattern.compile("(?m)^[ \\t]*static\\s*\\{");
        } else {
            String cleanMethodName = rawMethod.equals("<init>") ? "(?:[A-Z]\\w*)" : Pattern.quote(rawMethod);
            pattern = Pattern.compile("(?m)^[ \\t]*(?:@[\\w.]+(?:\\([^)]*\\))?\\s+)*(?:(?!\\b(?:class|interface|enum|record)\\b)[\\w<>\\[\\],?\\s])*\\b" + cleanMethodName + "\\s*(?:\\([^)]*\\))?\\s*[^{;]*(\\{|;)");
        }

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

            String matchText = matcher.group();
            String methodBlock;
            if (matchText.endsWith(";")) {
                methodBlock = fullSource.substring(startIdx, matcher.end()).trim();
            } else {
                int openBraceIdx = matcher.end() - 1;
                int depth = 0;
                int endIdx = -1;

                boolean inString = false;
                boolean inChar = false;
                boolean inLineComment = false;
                boolean inBlockComment = false;

                for (int i = openBraceIdx; i < fullSource.length(); i++) {
                    char c = fullSource.charAt(i);

                    if (inLineComment) {
                        if (c == '\n') inLineComment = false;
                        continue;
                    }
                    if (inBlockComment) {
                        if (c == '/' && i > openBraceIdx && fullSource.charAt(i - 1) == '*') inBlockComment = false;
                        continue;
                    }
                    if (inString) {
                        if (c == '"' && !isEscaped(fullSource, i)) inString = false;
                        continue;
                    }
                    if (inChar) {
                        if (c == '\'' && !isEscaped(fullSource, i)) inChar = false;
                        continue;
                    }

                    if (c == '/' && i + 1 < fullSource.length()) {
                        char next = fullSource.charAt(i + 1);
                        if (next == '/') { inLineComment = true; i++; continue; }
                        if (next == '*') { inBlockComment = true; i++; continue; }
                    }
                    if (c == '"') { inString = true; continue; }
                    if (c == '\'') { inChar = true; continue; }

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
                    methodBlock = fullSource.substring(startIdx, endIdx).trim();
                } else {
                    methodBlock = fullSource.substring(startIdx).trim();
                }
            }

            // If a specific parameter signature was requested, filter by matching argument count/types
            if (hasParamHint) {
                String hint = methodName.substring(methodName.indexOf('(') + 1, methodName.lastIndexOf(')')).trim();
                int openP = matchText.indexOf('(');
                int closeP = matchText.lastIndexOf(')');
                if (openP >= 0 && closeP > openP) {
                    String actualParams = matchText.substring(openP + 1, closeP);
                    List<String> hintTypes = splitTopLevelCommas(hint);
                    List<String> actualParamDecls = splitTopLevelCommas(actualParams);
                    if (hintTypes.size() == actualParamDecls.size()) {
                        boolean allTypesMatch = true;
                        for (int k = 0; k < hintTypes.size(); k++) {
                            String hType = hintTypes.get(k).trim();
                            if (!parameterMatchesType(actualParamDecls.get(k), hType)) {
                                allTypesMatch = false;
                                break;
                            }
                        }
                        if (allTypesMatch) {
                            return methodBlock;
                        }
                    }
                }
            }

            matchedMethods.add(methodBlock);
        }

        if (matchedMethods.isEmpty()) {
            return "// Method '" + methodName + "' not found in decompiled source.";
        }
        return String.join("\n\n", matchedMethods);
    }

    private static boolean parameterMatchesType(String decl, String hint) {
        String cleanDecl = decl != null ? decl.trim() : "";
        String cleanHint = hint != null ? hint.trim() : "";
        if (cleanDecl.isEmpty() || cleanHint.isEmpty()) {
            return cleanDecl.equals(cleanHint);
        }
        if (cleanDecl.equals(cleanHint)) {
            return true;
        }
        String escaped = Pattern.quote(cleanHint);
        String pattern = cleanHint.endsWith("[]")
                ? "(?:^|[^\\w$])" + escaped + "(?:[^\\w$]|$)"
                : "(?:^|[^\\w$])" + escaped + "(?![\\w$\\[])";
        return Pattern.compile(pattern).matcher(cleanDecl).find();
    }

    /**
     * Splits a parameter string by top-level commas, respecting nested parentheses,
     * generic angle brackets <...>, array brackets [...], and string literals.
     */
    private static List<String> splitTopLevelCommas(String s) {
        List<String> result = new ArrayList<>();
        if (s == null || s.trim().isEmpty()) {
            return result;
        }

        int depthParen = 0;
        int depthAngle = 0;
        int depthBracket = 0;
        boolean inStr = false;
        int start = 0;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && !isEscaped(s, i)) {
                inStr = !inStr;
                continue;
            }
            if (!inStr) {
                if (c == '(') depthParen++;
                else if (c == ')') depthParen = Math.max(0, depthParen - 1);
                else if (c == '<') depthAngle++;
                else if (c == '>') depthAngle = Math.max(0, depthAngle - 1);
                else if (c == '[') depthBracket++;
                else if (c == ']') depthBracket = Math.max(0, depthBracket - 1);
                else if (c == ',' && depthParen == 0 && depthAngle == 0 && depthBracket == 0) {
                    result.add(s.substring(start, i).trim());
                    start = i + 1;
                }
            }
        }
        if (start < s.length()) {
            String remainder = s.substring(start).trim();
            if (!remainder.isEmpty()) {
                result.add(remainder);
            }
        }
        return result;
    }
}
