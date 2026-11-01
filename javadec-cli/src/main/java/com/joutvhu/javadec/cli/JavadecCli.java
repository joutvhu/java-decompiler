package com.joutvhu.javadec.cli;

import com.joutvhu.javadec.core.api.DecompileOptions;
import com.joutvhu.javadec.core.api.JavaDecompiler;
import com.joutvhu.javadec.core.loader.PathUtil;
import com.joutvhu.javadec.core.model.ArchiveTree;
import com.joutvhu.javadec.core.model.ClassOutline;
import com.joutvhu.javadec.core.model.ExportResult;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
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
                JavadecCli.ResourceCommand.class,
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

        @Override
        public Integer call() throws Exception {
            File jar = PathUtil.resolveFile(jarFile);
            if (!jar.exists()) {
                System.err.println("Error: File not found: " + jar.getPath());
                return 1;
            }
            JavaDecompiler decompiler = new JavaDecompiler();
            List<String> classes = decompiler.listArchiveClasses(jar, pattern);
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

        @Override
        public Integer call() throws Exception {
            String resolvedTarget = target;
            if (classInJar != null && !classInJar.isBlank()) {
                resolvedTarget = target + "!" + classInJar;
            }

            JavaDecompiler decompiler = new JavaDecompiler();
            DecompileOptions options = DecompileOptions.builder()
                    .showLineNumbers(showLines)
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

        @Override
        public Integer call() throws Exception {
            File jar = PathUtil.resolveFile(jarFile);
            if (!jar.exists()) {
                System.err.println("Error: File not found: " + jar.getPath());
                return 1;
            }
            File outDir = PathUtil.resolveFile(outputDir);
            JavaDecompiler decompiler = new JavaDecompiler();
            DecompileOptions options = DecompileOptions.builder().showLineNumbers(showLines).build();
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
