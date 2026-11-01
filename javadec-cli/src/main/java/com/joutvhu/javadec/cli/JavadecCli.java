package com.joutvhu.javadec.cli;

import com.joutvhu.javadec.core.api.DecompileOptions;
import com.joutvhu.javadec.core.api.JavaDecompiler;
import com.joutvhu.javadec.core.model.ArchiveTree;
import com.joutvhu.javadec.core.model.ClassOutline;
import com.joutvhu.javadec.core.model.ExportResult;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
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
        int exitCode = new CommandLine(new JavadecCli()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
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
            JavaDecompiler decompiler = new JavaDecompiler();
            ArchiveTree tree = decompiler.inspectArchiveTree(jarFile, path, depth);
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
            JavaDecompiler decompiler = new JavaDecompiler();
            List<String> classes = decompiler.listArchiveClasses(jarFile, pattern);
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
            JavaDecompiler decompiler = new JavaDecompiler();
            ClassOutline outline;

            String resolvedTarget = target;
            if (classInJar != null && !classInJar.isBlank()) {
                resolvedTarget = target + "!" + classInJar;
            }

            if (resolvedTarget.contains("!")) {
                String[] parts = resolvedTarget.split("!", 2);
                outline = decompiler.getArchiveClassOutline(new File(parts[0]), parts[1]);
            } else {
                outline = decompiler.getClassOutline(new File(resolvedTarget));
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
            JavaDecompiler decompiler = new JavaDecompiler();
            DecompileOptions options = DecompileOptions.builder()
                    .showLineNumbers(showLines)
                    .lineOffset(offset)
                    .lineLimit(limit)
                    .build();

            String resolvedTarget = target;
            if (classInJar != null && !classInJar.isBlank()) {
                resolvedTarget = target + "!" + classInJar;
            }

            if (resolvedTarget.contains("!")) {
                String[] parts = resolvedTarget.split("!", 2);
                File jar = new File(parts[0]);
                String classPath = parts[1];

                if (method != null && !method.isBlank()) {
                    String result = decompiler.decompileArchiveMethod(jar, classPath, method, options);
                    System.out.println(result);
                } else {
                    String result = decompiler.decompileArchiveClass(jar, classPath, options);
                    System.out.println(result);
                }
            } else {
                File file = new File(resolvedTarget);
                if (method != null && !method.isBlank()) {
                    byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
                    String result = decompiler.decompileMethod(file.getName(), bytes, method, options);
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
            JavaDecompiler decompiler = new JavaDecompiler();
            String content = decompiler.readArchiveResource(jarFile, resourcePath, offset, limit);
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
            JavaDecompiler decompiler = new JavaDecompiler();
            DecompileOptions options = DecompileOptions.builder().showLineNumbers(showLines).build();
            ExportResult result = decompiler.exportArchive(jarFile, outputDir, pathFilter, options);
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
            JavaDecompiler decompiler = new JavaDecompiler();
            int count = decompiler.extractArchive(jarFile, outputDir, pathFilter);
            System.out.println("Extracted " + count + " file(s) to " + outputDir.getAbsolutePath());
            return 0;
        }
    }
}
