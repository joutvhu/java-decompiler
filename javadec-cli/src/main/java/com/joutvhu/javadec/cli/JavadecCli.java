package com.joutvhu.javadec.cli;

import com.joutvhu.javadec.core.api.DecompileOptions;
import com.joutvhu.javadec.core.api.JavaDecompiler;
import com.joutvhu.javadec.core.loader.PathUtil;
import com.joutvhu.javadec.core.model.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "javadec",
        mixinStandardHelpOptions = true,
        version = "javadec 1.0.0",
        description = "Universal & Token-Efficient Java Decompiler & Bytecode Inspector for AI & Developers",
        subcommands = {
                JavadecCli.TreeCommand.class,
                JavadecCli.ListCommand.class,
                JavadecCli.OutlineCommand.class,
                JavadecCli.ViewCommand.class,
                JavadecCli.DisasmCommand.class,
                JavadecCli.ResourceCommand.class,
                JavadecCli.FindCommand.class,
                JavadecCli.CalleesCommand.class,
                JavadecCli.DepsCommand.class,
                JavadecCli.StringsCommand.class,
                JavadecCli.TraceCommand.class,
                JavadecCli.DiffCommand.class,
                JavadecCli.ManifestCommand.class,
                JavadecCli.ModuleCommand.class,
                JavadecCli.AuditCommand.class,
                JavadecCli.MetricsCommand.class,
                JavadecCli.HierarchyCommand.class,
                JavadecCli.VerifyCommand.class,
                JavadecCli.DumpCommand.class,
                JavadecCli.ExtractCommand.class
        }
)
public class JavadecCli implements Callable<Integer> {

    public static void main(String[] args) {
        // Force UTF-8 for console output to guarantee clean box-drawing characters on all OS
        try {
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));
        } catch (Exception ignored) {}

        int exitCode = new CommandLine(new JavadecCli()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    private static boolean isArchiveFile(File file) {
        if (file == null) return false;
        String name = file.getName().toLowerCase();
        return name.endsWith(".jar") || name.endsWith(".zip") || name.endsWith(".war") || name.endsWith(".ear");
    }

    @Command(name = "tree", description = "Display compact hierarchical structure and summary of an archive")
    static class TreeCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to .jar or .zip archive")
        private File jarFile;

        @Option(names = {"--path", "-p"}, description = "Starting sub-path/folder inside archive (e.g. META-INF, org/springframework)")
        private String path;

        @Option(names = {"--depth", "-d"}, description = "Maximum tree depth to render (default: 3)")
        private Integer depth;

        @Override
        public Integer call() throws Exception {
            File jar = PathUtil.resolveFile(jarFile);
            if (!jar.exists()) {
                System.err.println("Error: File not found: " + jar.getPath());
                return 1;
            }
            JavaDecompiler decompiler = new JavaDecompiler();
            ArchiveTree tree = decompiler.inspectArchiveTree(jar, path, depth);
            System.out.println(tree.formattedTree());
            return 0;
        }
    }

    @Command(name = "list", description = "List class entries inside a JAR or ZIP archive")
    static class ListCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to .jar or .zip archive")
        private File jarFile;

        @Option(names = {"-p", "--pattern"}, description = "Filter class names by substring or package")
        private String pattern;

        @Option(names = {"-a", "--annotation"}, description = "Filter classes by annotation (e.g. @RestController, @Entity, @Service)")
        private String annotation;

        @Override
        public Integer call() throws Exception {
            File jar = PathUtil.resolveFile(jarFile);
            if (!jar.exists()) {
                System.err.println("Error: File not found: " + jar.getPath());
                return 1;
            }
            JavaDecompiler decompiler = new JavaDecompiler();
            List<String> classes = decompiler.listArchiveClasses(jar, pattern, annotation);
            for (String cls : classes) {
                System.out.println(cls);
            }
            return 0;
        }
    }

    @Command(name = "outline", description = "Extract class skeleton/signatures to save AI context tokens")
    static class OutlineCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to .class file or <jarPath>!<classPath>")
        private String target;

        @Parameters(index = "1", arity = "0..1", description = "Optional class path inside archive if not using '!'")
        private String classInJar;

        @Override
        public Integer call() throws Exception {
            String resolvedTarget = target;
            if (classInJar != null && !classInJar.isBlank()) {
                resolvedTarget = target + "!" + classInJar;
            }

            JavaDecompiler decompiler = new JavaDecompiler();
            ClassOutline outline;

            int bangIdx = resolvedTarget.lastIndexOf('!');
            if (bangIdx >= 0) {
                File jar = PathUtil.resolveFile(resolvedTarget.substring(0, bangIdx));
                if (!jar.exists()) {
                    System.err.println("Error: Archive file not found: " + jar.getPath());
                    return 1;
                }
                outline = decompiler.getArchiveClassOutline(jar, resolvedTarget.substring(bangIdx + 1));
            } else {
                File file = PathUtil.resolveFile(resolvedTarget);
                if (!file.exists()) {
                    System.err.println("Error: File not found: " + file.getPath());
                    return 1;
                }
                if (isArchiveFile(file)) {
                    System.err.println("Error: '" + resolvedTarget + "' is an archive. Please specify a class inside the archive:");
                    System.err.println("  Example: javadec outline " + resolvedTarget + " com/example/MyClass.class");
                    System.err.println("  Or use 'javadec tree " + resolvedTarget + "' to inspect the archive structure.");
                    return 1;
                }
                outline = decompiler.getClassOutline(file);
            }

            System.out.println(outline.toSkeletonSource());
            return 0;
        }
    }

    @Command(name = "view", description = "Decompile full class or selectively inspect a single method with pagination")
    static class ViewCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to .class file or <jarPath>!<classPath>")
        private String target;

        @Parameters(index = "1", arity = "0..1", description = "Optional class path inside archive if not using '!'")
        private String classInJar;

        @Option(names = {"-m", "--method"}, description = "Decompile only this specific method")
        private String method;

        @Option(names = {"-l", "--lines"}, description = "Display bytecode line number mappings")
        private boolean showLines;

        @Option(names = {"--offset"}, description = "1-based starting line number to read from")
        private Integer offset;

        @Option(names = {"--limit"}, description = "Number of lines to read")
        private Integer limit;

        @Option(names = {"--deobfuscate", "--rename"}, description = "Rename illegal, obfuscated, or duplicated identifiers")
        private boolean deobfuscate;

        @Option(names = {"--hex"}, description = "Display integer and bitmask constants in hexadecimal")
        private boolean showHex;

        @Option(names = {"-cp", "--classpath"}, description = "Additional classpath (JARs, directory, or Maven GAV)")
        private String classpath;

        @Option(names = {"--no-deps"}, description = "Disable automatic dependency detection from ~/.m2 and pom.xml")
        private boolean noDeps;

        @Override
        public Integer call() throws Exception {
            String resolvedTarget = target;
            if (classInJar != null && !classInJar.isBlank()) {
                resolvedTarget = target + "!" + classInJar;
            }

            JavaDecompiler decompiler = new JavaDecompiler();
            DecompileOptions options = DecompileOptions.builder()
                    .showLineNumbers(showLines)
                    .deobfuscate(deobfuscate)
                    .showHexLiterals(showHex)
                    .extraClasspath(classpath)
                    .autoDetectDependencies(!noDeps)
                    .lineOffset(offset)
                    .lineLimit(limit)
                    .build();

            int bangIdx = resolvedTarget.lastIndexOf('!');
            if (bangIdx >= 0) {
                File jar = PathUtil.resolveFile(resolvedTarget.substring(0, bangIdx));
                if (!jar.exists()) {
                    System.err.println("Error: Archive file not found: " + jar.getPath());
                    return 1;
                }
                String classPath = resolvedTarget.substring(bangIdx + 1);

                if (method != null && !method.isBlank()) {
                    String result = decompiler.decompileArchiveMethod(jar, classPath, method, options);
                    System.out.println(result);
                } else {
                    String result = decompiler.decompileArchiveClass(jar, classPath, options);
                    System.out.println(result);
                }
            } else {
                File file = PathUtil.resolveFile(resolvedTarget);
                if (!file.exists()) {
                    System.err.println("Error: File not found: " + file.getPath());
                    return 1;
                }
                if (isArchiveFile(file)) {
                    System.err.println("Error: '" + resolvedTarget + "' is an archive. Please specify a class inside the archive:");
                    System.err.println("  Example: javadec view " + resolvedTarget + " com/example/MyClass.class");
                    System.err.println("  Or use 'javadec dump " + resolvedTarget + " -o <outputDir>' to decompile all classes.");
                    return 1;
                }

                if (method != null && !method.isBlank()) {
                    String result = decompiler.decompileFileMethod(file, method, options);
                    System.out.println(result);
                } else {
                    String result = decompiler.decompileFile(file, options);
                    System.out.println(result);
                }
            }
            return 0;
        }
    }

    @Command(name = "disasm", aliases = {"bytecode", "ops"}, description = "Disassemble class or method bytecode into readable JVM instructions and opcodes")
    static class DisasmCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to .class file, <jarPath>!<classPath>, or GAV: <g:a:v>!<classPath>")
        private String target;

        @Parameters(index = "1", arity = "0..1", description = "Optional class path inside archive if not using '!'")
        private String classInJar;

        @Option(names = {"-m", "--method"}, description = "Disassemble only this specific method")
        private String method;

        @Option(names = {"--offset"}, description = "1-based starting line number to read from")
        private Integer offset;

        @Option(names = {"--limit"}, description = "Number of lines to read")
        private Integer limit;

        @Override
        public Integer call() throws Exception {
            String resolvedTarget = target;
            if (classInJar != null && !classInJar.isBlank()) {
                resolvedTarget = target + "!" + classInJar;
            }

            JavaDecompiler decompiler = new JavaDecompiler();

            int bangIdx = resolvedTarget.lastIndexOf('!');
            if (bangIdx >= 0) {
                File jar = PathUtil.resolveFile(resolvedTarget.substring(0, bangIdx));
                if (!jar.exists()) {
                    System.err.println("Error: Archive file not found: " + jar.getPath());
                    return 1;
                }
                String classPath = resolvedTarget.substring(bangIdx + 1);
                String result = decompiler.disassembleArchiveClass(jar, classPath, method, offset, limit);
                System.out.println(result);
            } else {
                File file = PathUtil.resolveFile(resolvedTarget);
                if (!file.exists()) {
                    System.err.println("Error: File not found: " + file.getPath());
                    return 1;
                }
                if (isArchiveFile(file)) {
                    System.err.println("Error: '" + resolvedTarget + "' is an archive. Please specify a class inside the archive:");
                    System.err.println("  Example: javadec disasm " + resolvedTarget + " com/example/MyClass.class");
                    return 1;
                }
                String result = decompiler.disassembleFile(file, method, offset, limit);
                System.out.println(result);
            }
            return 0;
        }
    }

    @Command(name = "resource", description = "Read non-class resource text (.properties, .yml, .xml, MANIFEST.MF, etc.) directly from archive with pagination")
    static class ResourceCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to .jar or .zip archive")
        private File jarFile;

        @Parameters(index = "1", description = "Relative resource path (e.g. META-INF/MANIFEST.MF, application.yml)")
        private String resourcePath;

        @Option(names = {"--offset"}, description = "1-based starting line number to read from")
        private Integer offset;

        @Option(names = {"--limit"}, description = "Number of lines to read")
        private Integer limit;

        @Override
        public Integer call() throws Exception {
            File jar = PathUtil.resolveFile(jarFile);
            if (!jar.exists()) {
                System.err.println("Error: File not found: " + jar.getPath());
                return 1;
            }
            JavaDecompiler decompiler = new JavaDecompiler();
            String content = decompiler.readArchiveResource(jar, resourcePath, offset, limit);
            System.out.println(content);
            return 0;
        }
    }

    @Command(name = "find", aliases = {"refs", "usages"}, description = "Find all callers, usages, or references of a method, field, or class in an archive")
    static class FindCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to .jar or .zip archive or directory")
        private File targetFile;

        @Parameters(index = "1", description = "Target to find, e.g. 'com/example/Service#method', '#methodName', or 'com/example/MyClass'")
        private String targetQuery;

        @Option(names = {"-l", "--limit"}, description = "Maximum number of references to return (default: 100)")
        private Integer limit;

        @Option(names = {"-p", "--path"}, description = "Optional package or path filter inside archive")
        private String pathFilter;

        @Override
        public Integer call() throws Exception {
            File file = PathUtil.resolveFile(targetFile);
            if (!file.exists()) {
                System.err.println("Error: File not found: " + file.getPath());
                return 1;
            }

            JavaDecompiler decompiler = new JavaDecompiler();
            ReferenceSearchResult result;
            if (file.isDirectory()) {
                result = decompiler.findReferencesInDirectory(file, targetQuery, limit);
            } else {
                result = decompiler.findReferences(file, targetQuery, limit, pathFilter);
            }

            System.out.println(result.formattedSummary());
            return 0;
        }
    }

    @Command(name = "callees", aliases = {"calls", "outgoing"}, description = "Extract outgoing method calls (callees) called from within a class or method")
    static class CalleesCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to .class file or <jarPath>!<classPath> or GAV")
        private String target;

        @Parameters(index = "1", arity = "0..1", description = "Optional class path inside archive if not using '!'")
        private String classInJar;

        @Option(names = {"-m", "--method"}, description = "Optional method name to inspect exclusively")
        private String method;

        @Option(names = {"-l", "--limit"}, description = "Maximum number of outgoing calls to return (default: 200)")
        private Integer limit;

        @Override
        public Integer call() throws Exception {
            String resolvedTarget = target;
            if (classInJar != null && !classInJar.isBlank()) {
                resolvedTarget = target + "!" + classInJar;
            }

            JavaDecompiler decompiler = new JavaDecompiler();
            CalleeResult result;

            int bangIdx = resolvedTarget.lastIndexOf('!');
            if (bangIdx >= 0) {
                File jar = PathUtil.resolveFile(resolvedTarget.substring(0, bangIdx));
                if (!jar.exists()) {
                    System.err.println("Error: Archive file not found: " + jar.getPath());
                    return 1;
                }
                result = decompiler.getArchiveCallees(jar, resolvedTarget.substring(bangIdx + 1), method, limit);
            } else {
                File file = PathUtil.resolveFile(resolvedTarget);
                if (!file.exists()) {
                    System.err.println("Error: File not found: " + file.getPath());
                    return 1;
                }
                byte[] bytes = Files.readAllBytes(file.toPath());
                result = decompiler.getCallees(bytes, method, limit);
            }

            System.out.println(result.formattedSummary());
            return 0;
        }
    }

    @Command(name = "deps", aliases = {"dependencies", "imports"}, description = "Analyze and categorize dependencies and imported classes (JDK, internal, third-party)")
    static class DepsCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to .class file or <jarPath>!<classPath> or GAV")
        private String target;

        @Parameters(index = "1", arity = "0..1", description = "Optional class path inside archive if not using '!'")
        private String classInJar;

        @Override
        public Integer call() throws Exception {
            String resolvedTarget = target;
            if (classInJar != null && !classInJar.isBlank()) {
                resolvedTarget = target + "!" + classInJar;
            }

            JavaDecompiler decompiler = new JavaDecompiler();
            DependencyAnalysisResult result;

            int bangIdx = resolvedTarget.lastIndexOf('!');
            if (bangIdx >= 0) {
                File jar = PathUtil.resolveFile(resolvedTarget.substring(0, bangIdx));
                if (!jar.exists()) {
                    System.err.println("Error: Archive file not found: " + jar.getPath());
                    return 1;
                }
                result = decompiler.analyzeArchiveDependencies(jar, resolvedTarget.substring(bangIdx + 1));
            } else {
                File file = PathUtil.resolveFile(resolvedTarget);
                if (!file.exists()) {
                    System.err.println("Error: File not found: " + file.getPath());
                    return 1;
                }
                byte[] bytes = Files.readAllBytes(file.toPath());
                result = decompiler.analyzeDependencies(bytes);
            }

            System.out.println(result.formattedReport());
            return 0;
        }
    }

    @Command(name = "strings", aliases = {"constants"}, description = "Extract hardcoded string constants (URLs, SQL, tokens, error messages) from an archive or class")
    static class StringsCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to .jar, .class, or GAV coordinate")
        private String target;

        @Option(names = {"-f", "--filter"}, description = "Optional substring to filter strings (case-insensitive)")
        private String filter;

        @Option(names = {"--min-len"}, description = "Minimum string length (default: 3)")
        private Integer minLength;

        @Option(names = {"-l", "--limit"}, description = "Maximum number of strings to return (default: 200)")
        private Integer limit;

        @Option(names = {"-p", "--path"}, description = "Optional class path/package filter inside archive")
        private String pathFilter;

        @Override
        public Integer call() throws Exception {
            JavaDecompiler decompiler = new JavaDecompiler();
            File file = PathUtil.resolveFile(target);
            if (!file.exists()) {
                System.err.println("Error: File not found: " + file.getPath());
                return 1;
            }

            ConstantStringResult result;
            if (isArchiveFile(file)) {
                result = decompiler.extractStrings(file, pathFilter, filter, minLength, limit);
            } else {
                byte[] bytes = Files.readAllBytes(file.toPath());
                result = decompiler.extractStringsFromClass(bytes, file.getName(), filter, minLength, limit);
            }
            System.out.println(result.formattedSummary());
            return 0;
        }
    }

    @Command(name = "trace", aliases = {"stack"}, description = "Locate and extract source context surrounding a specific line from a stack trace or log")
    static class TraceCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to .class file or <jarPath>!<classPath> (or GAV)")
        private String target;

        @Parameters(index = "1", arity = "0..1", description = "Line number (or specify target as <path>:<line>)")
        private String lineArg;

        @Option(names = {"-C", "--context"}, description = "Number of surrounding context lines to display (default: 3)")
        private Integer contextLines;

        @Override
        public Integer call() throws Exception {
            String resolvedTarget = target;
            int targetLine = -1;

            if (lineArg != null && !lineArg.isBlank()) {
                if (lineArg.contains(":")) {
                    resolvedTarget = target + "!" + lineArg.substring(0, lineArg.indexOf(':'));
                    targetLine = Integer.parseInt(lineArg.substring(lineArg.indexOf(':') + 1));
                } else {
                    targetLine = Integer.parseInt(lineArg);
                }
            } else if (target.contains(":")) {
                int colonIdx = target.lastIndexOf(':');
                targetLine = Integer.parseInt(target.substring(colonIdx + 1));
                resolvedTarget = target.substring(0, colonIdx);
            }

            if (targetLine <= 0) {
                System.err.println("Error: Please provide a valid 1-based line number (e.g. javadec trace <target> 45)");
                return 1;
            }

            JavaDecompiler decompiler = new JavaDecompiler();
            int bangIdx = resolvedTarget.lastIndexOf('!');
            StackTraceSnippetResult snippet;

            if (bangIdx >= 0) {
                File jar = PathUtil.resolveFile(resolvedTarget.substring(0, bangIdx));
                if (!jar.exists()) {
                    System.err.println("Error: Archive file not found: " + jar.getPath());
                    return 1;
                }
                snippet = decompiler.inspectStackTraceLine(jar, resolvedTarget.substring(bangIdx + 1), targetLine, contextLines);
            } else {
                File file = PathUtil.resolveFile(resolvedTarget);
                if (!file.exists()) {
                    System.err.println("Error: File not found: " + file.getPath());
                    return 1;
                }
                snippet = decompiler.inspectStackTraceLine(null, file.getPath(), targetLine, contextLines);
            }

            System.out.println(snippet.snippet());
            return 0;
        }
    }

    @Command(name = "diff", aliases = {"delta"}, description = "Compare binary public API differences (breaking changes, added/removed/modified methods) between two JARs or library versions")
    static class DiffCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to first JAR or Maven coordinate (e.g. org.ow2.asm:asm:9.6)")
        private String target1;

        @Parameters(index = "1", description = "Path to second JAR or Maven coordinate (e.g. org.ow2.asm:asm:9.7)")
        private String target2;

        @Option(names = {"-p", "--path"}, description = "Optional package filter (e.g. org.objectweb.asm)")
        private String pathFilter;

        @Option(names = {"--breaking-only"}, description = "Report only breaking changes (removed or modified public APIs)")
        private boolean breakingOnly;

        @Override
        public Integer call() throws Exception {
            File jar1 = PathUtil.resolveFile(target1);
            if (!jar1.exists()) {
                System.err.println("Error: First archive not found: " + target1);
                return 1;
            }
            File jar2 = PathUtil.resolveFile(target2);
            if (!jar2.exists()) {
                System.err.println("Error: Second archive not found: " + target2);
                return 1;
            }

            JavaDecompiler decompiler = new JavaDecompiler();
            JarApiDiffResult diff = decompiler.compareJarApis(jar1, jar2, pathFilter, breakingOnly);
            System.out.println(diff.formattedReport());
            return 0;
        }
    }

    @Command(name = "manifest", description = "Inspect structured packaging metadata, MANIFEST.MF attributes, and SPI services of an archive")
    static class ManifestCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to .jar or .zip archive or Maven GAV")
        private String target;

        @Override
        public Integer call() throws Exception {
            File jar = PathUtil.resolveFile(target);
            if (!jar.exists()) {
                System.err.println("Error: File not found: " + jar.getPath());
                return 1;
            }

            JavaDecompiler decompiler = new JavaDecompiler();
            ManifestSummary summary = decompiler.inspectManifest(jar);
            System.out.println(summary.formattedSummary());
            return 0;
        }
    }

    @Command(name = "module", aliases = {"jpms"}, description = "Inspect and extract the JPMS module declaration (requires, exports, opens, uses, provides) from an archive")
    static class ModuleCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to .jar or .zip archive or Maven GAV")
        private String target;

        @Override
        public Integer call() throws Exception {
            File jar = PathUtil.resolveFile(target);
            if (!jar.exists()) {
                System.err.println("Error: File not found: " + jar.getPath());
                return 1;
            }

            JavaDecompiler decompiler = new JavaDecompiler();
            String moduleInfo = decompiler.inspectModule(jar);
            System.out.println(moduleInfo);
            return 0;
        }
    }

    @Command(name = "audit", aliases = {"security", "check"}, description = "Audit archive or class bytecode for security vulnerabilities and risky anti-patterns")
    static class AuditCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to .jar, .class, or Maven GAV")
        private String target;

        @Option(names = {"--min-severity"}, description = "Filter findings by minimum severity: CRITICAL, HIGH, MEDIUM, LOW")
        private SecurityFindingItem.Severity minSeverity;

        @Option(names = {"-p", "--path"}, description = "Optional package or path filter inside archive")
        private String pathFilter;

        @Override
        public Integer call() throws Exception {
            File file = PathUtil.resolveFile(target);
            if (!file.exists()) {
                System.err.println("Error: File not found: " + file.getPath());
                return 1;
            }

            JavaDecompiler decompiler = new JavaDecompiler();
            SecurityAuditResult result;
            if (isArchiveFile(file)) {
                result = decompiler.auditSecurity(file, minSeverity, pathFilter);
            } else {
                byte[] bytes = Files.readAllBytes(file.toPath());
                result = decompiler.auditClassSecurity(bytes, file.getName(), minSeverity);
            }

            System.out.println(result.formattedSummary());
            return 0;
        }
    }

    @Command(name = "metrics", aliases = {"stats"}, description = "Inspect JVM technical bytecode metrics (instructions count, maxStack, maxLocals, tryCatch blocks)")
    static class MetricsCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to .class file or <jarPath>!<classPath> or GAV")
        private String target;

        @Parameters(index = "1", arity = "0..1", description = "Optional class path inside archive if not using '!'")
        private String classInJar;

        @Option(names = {"-m", "--method"}, description = "Optional method name to inspect exclusively")
        private String method;

        @Override
        public Integer call() throws Exception {
            String resolvedTarget = target;
            if (classInJar != null && !classInJar.isBlank()) {
                resolvedTarget = target + "!" + classInJar;
            }

            JavaDecompiler decompiler = new JavaDecompiler();
            ClassMetricsResult result;

            int bangIdx = resolvedTarget.lastIndexOf('!');
            if (bangIdx >= 0) {
                File jar = PathUtil.resolveFile(resolvedTarget.substring(0, bangIdx));
                if (!jar.exists()) {
                    System.err.println("Error: Archive file not found: " + jar.getPath());
                    return 1;
                }
                result = decompiler.extractArchiveMetrics(jar, resolvedTarget.substring(bangIdx + 1), method);
            } else {
                File file = PathUtil.resolveFile(resolvedTarget);
                if (!file.exists()) {
                    System.err.println("Error: File not found: " + file.getPath());
                    return 1;
                }
                byte[] bytes = Files.readAllBytes(file.toPath());
                result = decompiler.extractMetrics(bytes, method);
            }

            System.out.println(result.formattedSummary());
            return 0;
        }
    }

    @Command(name = "hierarchy", aliases = {"types", "tree-class"}, description = "Display full inheritance chain (superclasses up to Object) and all implemented interfaces")
    static class HierarchyCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to .class file or <jarPath>!<classPath> or GAV")
        private String target;

        @Parameters(index = "1", arity = "0..1", description = "Optional class path inside archive if not using '!'")
        private String classInJar;

        @Override
        public Integer call() throws Exception {
            String resolvedTarget = target;
            if (classInJar != null && !classInJar.isBlank()) {
                resolvedTarget = target + "!" + classInJar;
            }

            JavaDecompiler decompiler = new JavaDecompiler();
            ClassHierarchyResult result;

            int bangIdx = resolvedTarget.lastIndexOf('!');
            if (bangIdx >= 0) {
                File jar = PathUtil.resolveFile(resolvedTarget.substring(0, bangIdx));
                if (!jar.exists()) {
                    System.err.println("Error: Archive file not found: " + jar.getPath());
                    return 1;
                }
                result = decompiler.getArchiveClassHierarchy(jar, resolvedTarget.substring(bangIdx + 1));
            } else {
                File file = PathUtil.resolveFile(resolvedTarget);
                if (!file.exists()) {
                    System.err.println("Error: File not found: " + file.getPath());
                    return 1;
                }
                byte[] bytes = Files.readAllBytes(file.toPath());
                result = decompiler.getClassHierarchy(bytes, file.getName());
            }

            System.out.println(result.formattedHierarchy());
            return 0;
        }
    }

    @Command(name = "verify", aliases = {"signature"}, description = "Verify digital signature, certificate chain, and file integrity of a JAR archive")
    static class VerifyCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to .jar file or Maven GAV")
        private String target;

        @Override
        public Integer call() throws Exception {
            File jar = PathUtil.resolveFile(target);
            if (!jar.exists()) {
                System.err.println("Error: File not found: " + jar.getPath());
                return 1;
            }

            JavaDecompiler decompiler = new JavaDecompiler();
            JarSignatureResult result = decompiler.verifyJarSignature(jar);
            System.out.println(result.formattedSummary());
            return 0;
        }
    }

    @Command(name = "dump", aliases = {"export"}, description = "Decompile entire archive or filtered packages into .java files on disk")
    static class DumpCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to .jar or .zip archive")
        private File jarFile;

        @Option(names = {"-o", "--output"}, required = true, description = "Output directory for decompiled .java source files")
        private File outputDir;

        @Option(names = {"-p", "--path"}, description = "Optional package or path filter (e.g. com/example/service)")
        private String pathFilter;

        @Option(names = {"-l", "--lines"}, description = "Include bytecode line number comments in output")
        private boolean showLines;

        @Option(names = {"--deobfuscate", "--rename"}, description = "Rename illegal, obfuscated, or duplicated identifiers")
        private boolean deobfuscate;

        @Option(names = {"--hex"}, description = "Display integer and bitmask constants in hexadecimal")
        private boolean showHex;

        @Option(names = {"-cp", "--classpath"}, description = "Additional classpath (JARs, directory, or Maven GAV)")
        private String classpath;

        @Option(names = {"--no-deps"}, description = "Disable automatic dependency detection from ~/.m2 and pom.xml")
        private boolean noDeps;

        @Override
        public Integer call() throws Exception {
            File jar = PathUtil.resolveFile(jarFile);
            if (!jar.exists()) {
                System.err.println("Error: File not found: " + jar.getPath());
                return 1;
            }
            File outDir = PathUtil.resolveFile(outputDir);
            JavaDecompiler decompiler = new JavaDecompiler();
            DecompileOptions options = DecompileOptions.builder()
                    .showLineNumbers(showLines)
                    .deobfuscate(deobfuscate)
                    .showHexLiterals(showHex)
                    .extraClasspath(classpath)
                    .autoDetectDependencies(!noDeps)
                    .build();
            ExportResult result = decompiler.exportArchive(jar, outDir, pathFilter, options);
            System.out.println(result.toSummaryString());
            return 0;
        }
    }

    @Command(name = "extract", description = "Extract raw archive entries to a directory on disk")
    static class ExtractCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to .jar or .zip archive")
        private File jarFile;

        @Option(names = {"-o", "--output"}, required = true, description = "Destination directory")
        private File outputDir;

        @Option(names = {"-p", "--path"}, description = "Filter path prefix or filename")
        private String pathFilter;

        @Override
        public Integer call() throws Exception {
            File jar = PathUtil.resolveFile(jarFile);
            if (!jar.exists()) {
                System.err.println("Error: File not found: " + jar.getPath());
                return 1;
            }
            File outDir = PathUtil.resolveFile(outputDir);
            JavaDecompiler decompiler = new JavaDecompiler();
            int count = decompiler.extractArchive(jar, outDir, pathFilter);
            System.out.println("Extracted " + count + " file(s) to " + outDir.getAbsolutePath());
            return 0;
        }
    }
}
