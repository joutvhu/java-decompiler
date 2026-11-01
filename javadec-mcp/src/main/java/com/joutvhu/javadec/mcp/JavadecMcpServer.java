package com.joutvhu.javadec.mcp;

import com.joutvhu.javadec.core.api.DecompileOptions;
import com.joutvhu.javadec.core.api.JavaDecompiler;
import com.joutvhu.javadec.core.loader.PathUtil;
import com.joutvhu.javadec.core.model.ArchiveTree;
import com.joutvhu.javadec.core.model.ClassOutline;
import com.joutvhu.javadec.core.model.ExportResult;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Standard MCP Server implementation powered by official Model Context Protocol Java SDK
 * (io.modelcontextprotocol.sdk).
 */
public class JavadecMcpServer {

    private static final JavaDecompiler DECOMPILER = new JavaDecompiler();

    static boolean isArchiveFile(String path) {
        if (path == null) return false;
        String lower = path.toLowerCase();
        return lower.endsWith(".jar") || lower.endsWith(".zip") || lower.endsWith(".war") || lower.endsWith(".ear");
    }

    static String resolveTarget(Map<String, Object> arguments) {
        if (arguments.get("target") instanceof String t && !t.isBlank()) {
            return PathUtil.expandUserHome(t);
        }
        if (arguments.get("jarPath") instanceof String jar && arguments.get("classPath") instanceof String cls) {
            return PathUtil.expandUserHome(jar) + "!" + cls.trim();
        }
        return null;
    }

    static void validateTarget(String target) throws IOException {
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException("Either 'target' (e.g. 'library.jar!com/example/MyClass.class') or ('jarPath' and 'classPath') must be specified.");
        }
        int bangIdx = target.lastIndexOf('!');
        if (bangIdx >= 0) {
            String jarPath = target.substring(0, bangIdx);
            File jar = PathUtil.resolveFile(jarPath);
            if (!jar.exists()) {
                throw new FileNotFoundException("Archive file not found: " + jarPath);
            }
        } else {
            File f = PathUtil.resolveFile(target);
            if (!f.exists()) {
                throw new FileNotFoundException("File not found: " + target);
            }
            if (isArchiveFile(target)) {
                throw new IllegalArgumentException("Target '" + target + "' is an archive. Please specify a class inside the archive (e.g. target: '" + target + "!com/example/MyClass.class' or supply 'classPath').");
            }
        }
    }

    public static void main(String[] args) {
        AtomicBoolean closed = new AtomicBoolean(false);
        final McpSyncServer[] serverHolder = new McpSyncServer[1];

        Runnable shutdownAction = () -> {
            if (closed.compareAndSet(false, true)) {
                if (serverHolder[0] != null) {
                    try {
                        serverHolder[0].close();
                    } catch (Exception ignored) {}
                }
            }
        };

        // Wrap System.in to detect client disconnect (stdin EOF) and exit cleanly without hanging JVM
        InputStream wrappedIn = new FilterInputStream(System.in) {
            private final AtomicBoolean triggered = new AtomicBoolean(false);

            private void onEof() {
                if (triggered.compareAndSet(false, true)) {
                    new Thread(() -> {
                        shutdownAction.run();
                        System.exit(0);
                    }, "javadec-mcp-eof-shutdown").start();
                }
            }

            @Override
            public int read() throws IOException {
                int b = super.read();
                if (b == -1) {
                    onEof();
                }
                return b;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int n = super.read(b, off, len);
                if (n == -1) {
                    onEof();
                }
                return n;
            }
        };

        StdioServerTransportProvider transportProvider =
                new StdioServerTransportProvider(McpJsonDefaults.getMapper(), wrappedIn, System.out);

        McpSyncServer server = McpServer.sync(transportProvider)
                .serverInfo("javadec-mcp", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("inspect_archive_tree")
                                .description("Inspect archive structure, returning a hierarchical tree with optional root folder filter and max depth")
                                .inputSchema(Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "jarPath", Map.of("type", "string", "description", "Path to .jar or .zip archive (supports ~ and $HOME)"),
                                                "path", Map.of("type", "string", "description", "Optional sub-path/folder inside archive (e.g. META-INF, org/springframework)"),
                                                "depth", Map.of("type", "integer", "description", "Maximum tree depth to render (default: 3)")
                                        ),
                                        "required", List.of("jarPath")
                                ))
                                .build(),
                        (exchange, request) -> {
                            try {
                                String jarPath = (String) request.arguments().get("jarPath");
                                File jar = PathUtil.resolveFile(jarPath);
                                if (!jar.exists()) {
                                    throw new FileNotFoundException("Archive file not found: " + jarPath);
                                }

                                String path = PathUtil.expandUserHome((String) request.arguments().get("path"));
                                Integer depth = request.arguments().get("depth") instanceof Number n ? n.intValue() : null;
                                ArchiveTree tree = DECOMPILER.inspectArchiveTree(jar, path, depth);
                                return McpSchema.CallToolResult.builder()
                                        .content(List.of(new McpSchema.TextContent(tree.formattedTree())))
                                        .isError(false)
                                        .build();
                            } catch (Exception e) {
                                return McpSchema.CallToolResult.builder()
                                        .content(List.of(new McpSchema.TextContent("Error: " + e.getMessage())))
                                        .isError(true)
                                        .build();
                            }
                        }
                )
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("list_jar_classes")
                                .description("List class/package entries inside a .jar or .zip archive")
                                .inputSchema(Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "jarPath", Map.of("type", "string", "description", "Path to .jar or .zip archive (supports ~ and $HOME)"),
                                                "pattern", Map.of("type", "string", "description", "Optional substring or package filter for class names")
                                        ),
                                        "required", List.of("jarPath")
                                ))
                                .build(),
                        (exchange, request) -> {
                            try {
                                String jarPath = (String) request.arguments().get("jarPath");
                                File jar = PathUtil.resolveFile(jarPath);
                                if (!jar.exists()) {
                                    throw new FileNotFoundException("Archive file not found: " + jarPath);
                                }

                                String pattern = (String) request.arguments().get("pattern");
                                List<String> list = DECOMPILER.listArchiveClasses(jar, pattern);
                                String textResult = list.isEmpty()
                                        ? (pattern != null && !pattern.isBlank() ? "// No classes found matching pattern '" + pattern + "' in archive: " + jarPath : "// No classes found in archive: " + jarPath)
                                        : String.join("\n", list);
                                return McpSchema.CallToolResult.builder()
                                        .content(List.of(new McpSchema.TextContent(textResult)))
                                        .isError(false)
                                        .build();
                            } catch (Exception e) {
                                return McpSchema.CallToolResult.builder()
                                        .content(List.of(new McpSchema.TextContent("Error: " + e.getMessage())))
                                        .isError(true)
                                        .build();
                            }
                        }
                )
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("read_jar_resource")
                                .description("Read text content of any resource/configuration file (e.g. .yml, .properties, .xml, MANIFEST.MF) with optional line pagination (offset/limit)")
                                .inputSchema(Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "jarPath", Map.of("type", "string", "description", "Path to .jar or .zip archive (supports ~ and $HOME)"),
                                                "resourcePath", Map.of("type", "string", "description", "Relative resource path inside archive (e.g. META-INF/MANIFEST.MF, application.yml)"),
                                                "lineOffset", Map.of("type", "integer", "description", "Optional 1-based start line number to read from"),
                                                "lineLimit", Map.of("type", "integer", "description", "Optional number of lines to read")
                                        ),
                                        "required", List.of("jarPath", "resourcePath")
                                ))
                                .build(),
                        (exchange, request) -> {
                            try {
                                String jarPath = (String) request.arguments().get("jarPath");
                                File jar = PathUtil.resolveFile(jarPath);
                                if (!jar.exists()) {
                                    throw new FileNotFoundException("Archive file not found: " + jarPath);
                                }

                                String resourcePath = (String) request.arguments().get("resourcePath");
                                Integer lineOffset = request.arguments().get("lineOffset") instanceof Number n ? n.intValue() : null;
                                Integer lineLimit = request.arguments().get("lineLimit") instanceof Number n ? n.intValue() : null;
                                String content = DECOMPILER.readArchiveResource(jar, resourcePath, lineOffset, lineLimit);
                                return McpSchema.CallToolResult.builder()
                                        .content(List.of(new McpSchema.TextContent(content)))
                                        .isError(false)
                                        .build();
                            } catch (Exception e) {
                                return McpSchema.CallToolResult.builder()
                                        .content(List.of(new McpSchema.TextContent("Error: " + e.getMessage())))
                                        .isError(true)
                                        .build();
                            }
                        }
                )
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("get_class_outline")
                                .description("Extract class skeleton/signatures to inspect APIs with minimal token usage")
                                .inputSchema(Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "target", Map.of("type", "string", "description", "Path to .class file or <jarPath>!<classPath> (supports ~ and $HOME)"),
                                                "jarPath", Map.of("type", "string", "description", "Optional archive path (alternative to target URI)"),
                                                "classPath", Map.of("type", "string", "description", "Optional class path inside archive (alternative to target URI)")
                                        ),
                                        "anyOf", List.of(
                                                Map.of("required", List.of("target")),
                                                Map.of("required", List.of("jarPath", "classPath"))
                                        )
                                ))
                                .build(),
                        (exchange, request) -> {
                            try {
                                String target = resolveTarget(request.arguments());
                                validateTarget(target);

                                ClassOutline outline;
                                int bangIdx = target.lastIndexOf('!');
                                if (bangIdx >= 0) {
                                    File jar = PathUtil.resolveFile(target.substring(0, bangIdx));
                                    outline = DECOMPILER.getArchiveClassOutline(jar, target.substring(bangIdx + 1));
                                } else {
                                    File file = PathUtil.resolveFile(target);
                                    outline = DECOMPILER.getClassOutline(file);
                                }
                                return McpSchema.CallToolResult.builder()
                                        .content(List.of(new McpSchema.TextContent(outline.toSkeletonSource())))
                                        .isError(false)
                                        .build();
                            } catch (Exception e) {
                                return McpSchema.CallToolResult.builder()
                                        .content(List.of(new McpSchema.TextContent("Error: " + e.getMessage())))
                                        .isError(true)
                                        .build();
                            }
                        }
                )
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("decompile_class")
                                .description("Decompile full class source code from a .class file or <jarPath>!<classPath> with optional line pagination (offset/limit)")
                                .inputSchema(Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "target", Map.of("type", "string", "description", "Path to .class file or <jarPath>!<classPath> (supports ~ and $HOME)"),
                                                "jarPath", Map.of("type", "string", "description", "Optional archive path (alternative to target URI)"),
                                                "classPath", Map.of("type", "string", "description", "Optional class path inside archive (alternative to target URI)"),
                                                "lineOffset", Map.of("type", "integer", "description", "Optional 1-based start line number to read from"),
                                                "lineLimit", Map.of("type", "integer", "description", "Optional number of lines to read")
                                        ),
                                        "anyOf", List.of(
                                                Map.of("required", List.of("target")),
                                                Map.of("required", List.of("jarPath", "classPath"))
                                        )
                                ))
                                .build(),
                        (exchange, request) -> {
                            try {
                                String target = resolveTarget(request.arguments());
                                validateTarget(target);

                                Integer lineOffset = request.arguments().get("lineOffset") instanceof Number n ? n.intValue() : null;
                                Integer lineLimit = request.arguments().get("lineLimit") instanceof Number n ? n.intValue() : null;
                                DecompileOptions options = DecompileOptions.builder()
                                        .lineOffset(lineOffset)
                                        .lineLimit(lineLimit)
                                        .build();

                                String result;
                                int bangIdx = target.lastIndexOf('!');
                                if (bangIdx >= 0) {
                                    File jar = PathUtil.resolveFile(target.substring(0, bangIdx));
                                    result = DECOMPILER.decompileArchiveClass(jar, target.substring(bangIdx + 1), options);
                                } else {
                                    File file = PathUtil.resolveFile(target);
                                    result = DECOMPILER.decompileFile(file, options);
                                }
                                return McpSchema.CallToolResult.builder()
                                        .content(List.of(new McpSchema.TextContent(result)))
                                        .isError(false)
                                        .build();
                            } catch (Exception e) {
                                return McpSchema.CallToolResult.builder()
                                        .content(List.of(new McpSchema.TextContent("Error: " + e.getMessage())))
                                        .isError(true)
                                        .build();
                            }
                        }
                )
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("decompile_method")
                                .description("Selectively decompile only a single method inside a class to optimize AI context tokens with optional line pagination")
                                .inputSchema(Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "target", Map.of("type", "string", "description", "Path to .class file or <jarPath>!<classPath> (supports ~ and $HOME)"),
                                                "jarPath", Map.of("type", "string", "description", "Optional archive path (alternative to target URI)"),
                                                "classPath", Map.of("type", "string", "description", "Optional class path inside archive (alternative to target URI)"),
                                                "methodName", Map.of("type", "string", "description", "Name of the method to decompile"),
                                                "lineOffset", Map.of("type", "integer", "description", "Optional 1-based start line number to read from"),
                                                "lineLimit", Map.of("type", "integer", "description", "Optional number of lines to read")
                                        ),
                                        "required", List.of("methodName"),
                                        "anyOf", List.of(
                                                Map.of("required", List.of("target")),
                                                Map.of("required", List.of("jarPath", "classPath"))
                                        )
                                ))
                                .build(),
                        (exchange, request) -> {
                            try {
                                String target = resolveTarget(request.arguments());
                                validateTarget(target);

                                String methodName = (String) request.arguments().get("methodName");
                                Integer lineOffset = request.arguments().get("lineOffset") instanceof Number n ? n.intValue() : null;
                                Integer lineLimit = request.arguments().get("lineLimit") instanceof Number n ? n.intValue() : null;
                                DecompileOptions options = DecompileOptions.builder()
                                        .lineOffset(lineOffset)
                                        .lineLimit(lineLimit)
                                        .build();

                                String result;
                                int bangIdx = target.lastIndexOf('!');
                                if (bangIdx >= 0) {
                                    File jar = PathUtil.resolveFile(target.substring(0, bangIdx));
                                    result = DECOMPILER.decompileArchiveMethod(jar, target.substring(bangIdx + 1), methodName, options);
                                } else {
                                    File f = PathUtil.resolveFile(target);
                                    result = DECOMPILER.decompileFileMethod(f, methodName, options);
                                }
                                return McpSchema.CallToolResult.builder()
                                        .content(List.of(new McpSchema.TextContent(result)))
                                        .isError(false)
                                        .build();
                            } catch (Exception e) {
                                return McpSchema.CallToolResult.builder()
                                        .content(List.of(new McpSchema.TextContent("Error: " + e.getMessage())))
                                        .isError(true)
                                        .build();
                            }
                        }
                )
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("export_archive")
                                .description("Decompile all (or filtered) classes in an archive into .java files on disk, copying resources and maintaining package structure")
                                .inputSchema(Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "jarPath", Map.of("type", "string", "description", "Path to .jar or .zip archive (supports ~ and $HOME)"),
                                                "outputDir", Map.of("type", "string", "description", "Directory path on disk to output decompiled .java source files"),
                                                "pathFilter", Map.of("type", "string", "description", "Optional package or path filter (e.g. com/example/service)")
                                        ),
                                        "required", List.of("jarPath", "outputDir")
                                ))
                                .build(),
                        (exchange, request) -> {
                            try {
                                String jarPath = (String) request.arguments().get("jarPath");
                                File jar = PathUtil.resolveFile(jarPath);
                                if (!jar.exists()) {
                                    throw new FileNotFoundException("Archive file not found: " + jarPath);
                                }

                                String outputDir = (String) request.arguments().get("outputDir");
                                File outDir = PathUtil.resolveFile(outputDir);
                                String pathFilter = (String) request.arguments().get("pathFilter");
                                ExportResult result = DECOMPILER.exportArchive(jar, outDir, pathFilter, DecompileOptions.defaults());
                                return McpSchema.CallToolResult.builder()
                                        .content(List.of(new McpSchema.TextContent(result.toSummaryString())))
                                        .isError(false)
                                        .build();
                            } catch (Exception e) {
                                return McpSchema.CallToolResult.builder()
                                        .content(List.of(new McpSchema.TextContent("Error: " + e.getMessage())))
                                        .isError(true)
                                        .build();
                            }
                        }
                )
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("extract_archive")
                                .description("Extract raw files (bytecode and resources) from an archive to a destination directory on disk")
                                .inputSchema(Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "jarPath", Map.of("type", "string", "description", "Path to .jar or .zip archive (supports ~ and $HOME)"),
                                                "outputDir", Map.of("type", "string", "description", "Directory path on disk to extract files into"),
                                                "pathFilter", Map.of("type", "string", "description", "Optional path prefix or filename filter")
                                        ),
                                        "required", List.of("jarPath", "outputDir")
                                ))
                                .build(),
                        (exchange, request) -> {
                            try {
                                String jarPath = (String) request.arguments().get("jarPath");
                                File jar = PathUtil.resolveFile(jarPath);
                                if (!jar.exists()) {
                                    throw new FileNotFoundException("Archive file not found: " + jarPath);
                                }

                                String outputDir = (String) request.arguments().get("outputDir");
                                File outDir = PathUtil.resolveFile(outputDir);
                                String pathFilter = (String) request.arguments().get("pathFilter");
                                int count = DECOMPILER.extractArchive(jar, outDir, pathFilter);
                                return McpSchema.CallToolResult.builder()
                                        .content(List.of(new McpSchema.TextContent("Successfully extracted " + count + " file(s) to " + outDir.getAbsolutePath())))
                                        .isError(false)
                                        .build();
                            } catch (Exception e) {
                                return McpSchema.CallToolResult.builder()
                                        .content(List.of(new McpSchema.TextContent("Error: " + e.getMessage())))
                                        .isError(true)
                                        .build();
                            }
                        }
                )
                .build();

        serverHolder[0] = server;

        // Graceful shutdown hook coordinated idempotently
        Runtime.getRuntime().addShutdownHook(new Thread(shutdownAction));

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            shutdownAction.run();
        }
    }
}
