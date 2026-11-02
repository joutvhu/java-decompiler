package com.joutvhu.javadec.mcp;

import com.joutvhu.javadec.core.api.DecompileOptions;
import com.joutvhu.javadec.core.api.JavaDecompiler;
import com.joutvhu.javadec.core.loader.PathUtil;
import com.joutvhu.javadec.core.model.*;
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
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Standard MCP Server implementation powered by official Model Context Protocol Java SDK
 * (io.modelcontextprotocol.sdk).
 */
public class JavadecMcpServer {

    private static final JavaDecompiler DECOMPILER = new JavaDecompiler();

    private static final String SERVER_TITLE = "Java Decompiler & Archive Inspector";

    private static final String SERVER_DESCRIPTION =
            "Universal & Token-Efficient Java Bytecode, Resource, and Archive Inspector for AI agents";

    private static final String SERVER_INSTRUCTIONS =
            """
            Universal & Token-Efficient Java Bytecode, Resource, and Archive Inspector.
            Use whenever inspecting, reverse-engineering, exploring, decompiling, or exporting Java JAR archives, WARs, or .class files, especially when tracing external library stack traces, inspecting third-party API signatures, finding method/class callers and references, reading archive resources (manifest, YAML, properties), or verifying compiled bytecode.

            ## Token-First Hierarchy
            Always prefer minimal-token inspection before full decompilation:
            inspect_archive_tree -> read_jar_resource -> get_class_outline -> find_references -> decompile_method -> decompile_class -> export_archive / extract_archive

            ## Target URI Format
            - Class inside JAR archive: "<path/to/archive.jar>!<internal/package/ClassName.class>" (e.g. "~/.m2/repository/.../spring-core-6.1.0.jar!org/springframework/core/io/Resource.class")
            - Or use separate parameters: jarPath and classPath
            - Standalone .class file: "<path/to/ClassName.class>" (e.g. "target/classes/com/example/MyClass.class")

            ## Locating Dependencies
            - Maven local repository: ~/.m2/repository/<group/as/path>/<artifact>/<version>/<artifact>-<version>.jar
            - Gradle cache: ~/.gradle/caches/modules-2/files-2.1/<group>/<artifact>/<version>/...
            - Project build outputs: target/*.jar or build/libs/*.jar
            """;

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
                .serverInfo(McpSchema.Implementation.builder("javadec-mcp", "1.0.0")
                        .title(SERVER_TITLE)
                        .description(SERVER_DESCRIPTION)
                        .websiteUrl("https://github.com/joutvhu/java-decompiler")
                        .build())
                .instructions(SERVER_INSTRUCTIONS)
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
                                                "deobfuscate", Map.of("type", "boolean", "description", "Optional flag to rename illegal or obfuscated identifiers"),
                                                "showHexLiterals", Map.of("type", "boolean", "description", "Optional flag to format integer/bitmask constants as hexadecimal"),
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
                                boolean deobfuscate = Boolean.TRUE.equals(request.arguments().get("deobfuscate"));
                                boolean showHex = Boolean.TRUE.equals(request.arguments().get("showHexLiterals"));
                                DecompileOptions options = DecompileOptions.builder()
                                        .deobfuscate(deobfuscate)
                                        .showHexLiterals(showHex)
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
                                                "deobfuscate", Map.of("type", "boolean", "description", "Optional flag to rename illegal or obfuscated identifiers"),
                                                "showHexLiterals", Map.of("type", "boolean", "description", "Optional flag to format integer/bitmask constants as hexadecimal"),
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
                                boolean deobfuscate = Boolean.TRUE.equals(request.arguments().get("deobfuscate"));
                                boolean showHex = Boolean.TRUE.equals(request.arguments().get("showHexLiterals"));
                                DecompileOptions options = DecompileOptions.builder()
                                        .deobfuscate(deobfuscate)
                                        .showHexLiterals(showHex)
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
                                .name("disassemble_bytecode")
                                .description("Disassemble class or method bytecode into readable JVM instructions and opcodes (iload, invokevirtual, tableswitch, etc.)")
                                .inputSchema(Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "target", Map.of("type", "string", "description", "Path to .class file, <jarPath>!<classPath>, or GAV: <g:a:v>!<classPath>"),
                                                "jarPath", Map.of("type", "string", "description", "Optional archive path or GAV (alternative to target URI)"),
                                                "classPath", Map.of("type", "string", "description", "Optional class path inside archive (alternative to target URI)"),
                                                "methodName", Map.of("type", "string", "description", "Optional method name to disassemble exclusively"),
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

                                String methodName = (String) request.arguments().get("methodName");
                                Integer lineOffset = request.arguments().get("lineOffset") instanceof Number n ? n.intValue() : null;
                                Integer lineLimit = request.arguments().get("lineLimit") instanceof Number n ? n.intValue() : null;

                                String result;
                                int bangIdx = target.lastIndexOf('!');
                                if (bangIdx >= 0) {
                                    File jar = PathUtil.resolveFile(target.substring(0, bangIdx));
                                    result = DECOMPILER.disassembleArchiveClass(jar, target.substring(bangIdx + 1), methodName, lineOffset, lineLimit);
                                } else {
                                    File file = PathUtil.resolveFile(target);
                                    result = DECOMPILER.disassembleFile(file, methodName, lineOffset, lineLimit);
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
                                .name("find_references")
                                .description("Find all callers, usages, or references of a method, field, or class in an archive with line numbers")
                                .inputSchema(Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "jarPath", Map.of("type", "string", "description", "Path to .jar or .zip archive (supports ~ and $HOME)"),
                                                "target", Map.of("type", "string", "description", "Target to find, e.g. 'com/example/Service#method', '#methodName', or 'com/example/MyClass'"),
                                                "limit", Map.of("type", "integer", "description", "Optional maximum number of references to return (default: 100)"),
                                                "pathFilter", Map.of("type", "string", "description", "Optional package or path prefix filter inside archive")
                                        ),
                                        "required", List.of("jarPath", "target")
                                ))
                                .build(),
                        (exchange, request) -> {
                            try {
                                String jarPath = (String) request.arguments().get("jarPath");
                                File jar = PathUtil.resolveFile(jarPath);
                                if (!jar.exists()) {
                                    throw new FileNotFoundException("Archive file not found: " + jarPath);
                                }

                                String target = (String) request.arguments().get("target");
                                if (target == null || target.isBlank()) {
                                    throw new IllegalArgumentException("'target' query is required (e.g. 'com/example/MyService#myMethod' or 'com.example.MyClass')");
                                }

                                Integer limit = request.arguments().get("limit") instanceof Number n ? n.intValue() : 100;
                                String pathFilter = (String) request.arguments().get("pathFilter");
                                ReferenceSearchResult result = DECOMPILER.findReferences(jar, target, limit, pathFilter);
                                return McpSchema.CallToolResult.builder()
                                        .content(List.of(new McpSchema.TextContent(result.formattedSummary())))
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
                                .name("get_callees")
                                .description("Extract outgoing method calls (callees) called from within a class or specific method")
                                .inputSchema(Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "target", Map.of("type", "string", "description", "Path to .class file or <jarPath>!<classPath> or GAV"),
                                                "jarPath", Map.of("type", "string", "description", "Optional archive path or GAV (alternative to target URI)"),
                                                "classPath", Map.of("type", "string", "description", "Optional class path inside archive (alternative to target URI)"),
                                                "methodName", Map.of("type", "string", "description", "Optional method name to inspect exclusively"),
                                                "limit", Map.of("type", "integer", "description", "Optional maximum number of calls to return (default: 200)")
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

                                String methodName = (String) request.arguments().get("methodName");
                                Integer limit = request.arguments().get("limit") instanceof Number n ? n.intValue() : 200;

                                CalleeResult result;
                                int bangIdx = target.lastIndexOf('!');
                                if (bangIdx >= 0) {
                                    File jar = PathUtil.resolveFile(target.substring(0, bangIdx));
                                    result = DECOMPILER.getArchiveCallees(jar, target.substring(bangIdx + 1), methodName, limit);
                                } else {
                                    File file = PathUtil.resolveFile(target);
                                    byte[] bytes = Files.readAllBytes(file.toPath());
                                    result = DECOMPILER.getCallees(bytes, methodName, limit);
                                }
                                return McpSchema.CallToolResult.builder()
                                        .content(List.of(new McpSchema.TextContent(result.formattedSummary())))
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
                                .name("analyze_dependencies")
                                .description("Analyze and categorize all imported classes and packages referenced by a class (JDK, internal, third-party)")
                                .inputSchema(Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "target", Map.of("type", "string", "description", "Path to .class file or <jarPath>!<classPath> or GAV"),
                                                "jarPath", Map.of("type", "string", "description", "Optional archive path or GAV (alternative to target URI)"),
                                                "classPath", Map.of("type", "string", "description", "Optional class path inside archive (alternative to target URI)"),
                                                "lineOffset", Map.of("type", "integer", "description", "Optional start line offset"),
                                                "lineLimit", Map.of("type", "integer", "description", "Optional line count limit")
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

                                DependencyAnalysisResult result;
                                int bangIdx = target.lastIndexOf('!');
                                if (bangIdx >= 0) {
                                    File jar = PathUtil.resolveFile(target.substring(0, bangIdx));
                                    result = DECOMPILER.analyzeArchiveDependencies(jar, target.substring(bangIdx + 1));
                                } else {
                                    File file = PathUtil.resolveFile(target);
                                    byte[] bytes = Files.readAllBytes(file.toPath());
                                    result = DECOMPILER.analyzeDependencies(bytes);
                                }
                                return McpSchema.CallToolResult.builder()
                                        .content(List.of(new McpSchema.TextContent(result.formattedReport())))
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
                                .name("extract_jar_strings")
                                .description("Extract hardcoded string constants (URLs, SQL, tokens, error messages) directly from bytecode without decompilation")
                                .inputSchema(Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "jarPath", Map.of("type", "string", "description", "Path to .jar or .zip archive or Maven GAV coordinate"),
                                                "pathFilter", Map.of("type", "string", "description", "Optional class path/package prefix filter inside archive"),
                                                "textFilter", Map.of("type", "string", "description", "Optional substring to filter strings (case-insensitive)"),
                                                "minLength", Map.of("type", "integer", "description", "Minimum string length (default: 3)"),
                                                "limit", Map.of("type", "integer", "description", "Maximum number of strings to return (default: 200)")
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

                                String pathFilter = (String) request.arguments().get("pathFilter");
                                String textFilter = (String) request.arguments().get("textFilter");
                                Integer minLen = request.arguments().get("minLength") instanceof Number n ? n.intValue() : 3;
                                Integer limit = request.arguments().get("limit") instanceof Number n ? n.intValue() : 200;

                                ConstantStringResult result = DECOMPILER.extractStrings(jar, pathFilter, textFilter, minLen, limit);
                                return McpSchema.CallToolResult.builder()
                                        .content(List.of(new McpSchema.TextContent(result.formattedSummary())))
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
                                .name("inspect_stack_line")
                                .description("Locate and extract source code snippet surrounding a specific line number from a stack trace or log (~95% token savings)")
                                .inputSchema(Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "target", Map.of("type", "string", "description", "Path to .class file or <jarPath>!<classPath> or GAV"),
                                                "jarPath", Map.of("type", "string", "description", "Optional archive path or GAV (alternative to target URI)"),
                                                "classPath", Map.of("type", "string", "description", "Optional class path inside archive (alternative to target URI)"),
                                                "lineNumber", Map.of("type", "integer", "description", "1-based line number from exception stack trace"),
                                                "contextLines", Map.of("type", "integer", "description", "Number of surrounding context lines to include (default: 3)")
                                        ),
                                        "required", List.of("lineNumber"),
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

                                Integer lineNum = request.arguments().get("lineNumber") instanceof Number n ? n.intValue() : -1;
                                if (lineNum <= 0) {
                                    throw new IllegalArgumentException("'lineNumber' must be a positive integer.");
                                }
                                Integer contextLines = request.arguments().get("contextLines") instanceof Number n ? n.intValue() : 3;

                                StackTraceSnippetResult snippet;
                                int bangIdx = target.lastIndexOf('!');
                                if (bangIdx >= 0) {
                                    File jar = PathUtil.resolveFile(target.substring(0, bangIdx));
                                    snippet = DECOMPILER.inspectStackTraceLine(jar, target.substring(bangIdx + 1), lineNum, contextLines);
                                } else {
                                    File file = PathUtil.resolveFile(target);
                                    snippet = DECOMPILER.inspectStackTraceLine(null, file.getPath(), lineNum, contextLines);
                                }
                                return McpSchema.CallToolResult.builder()
                                        .content(List.of(new McpSchema.TextContent(snippet.snippet())))
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
                                .name("jar_diff")
                                .description("Compare binary public API differences (breaking changes, added/removed/modified methods) between two JARs or library versions")
                                .inputSchema(Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "jar1", Map.of("type", "string", "description", "First JAR path or Maven GAV coordinate (e.g. org.ow2.asm:asm:9.6)"),
                                                "jar2", Map.of("type", "string", "description", "Second JAR path or Maven GAV coordinate (e.g. org.ow2.asm:asm:9.7)"),
                                                "packageFilter", Map.of("type", "string", "description", "Optional package filter prefix"),
                                                "breakingOnly", Map.of("type", "boolean", "description", "Optional flag to report only breaking changes")
                                        ),
                                        "required", List.of("jar1", "jar2")
                                ))
                                .build(),
                        (exchange, request) -> {
                            try {
                                String jar1Str = (String) request.arguments().get("jar1");
                                String jar2Str = (String) request.arguments().get("jar2");
                                File jar1 = PathUtil.resolveFile(jar1Str);
                                File jar2 = PathUtil.resolveFile(jar2Str);

                                if (!jar1.exists()) {
                                    throw new FileNotFoundException("First archive not found: " + jar1Str);
                                }
                                if (!jar2.exists()) {
                                    throw new FileNotFoundException("Second archive not found: " + jar2Str);
                                }

                                String packageFilter = (String) request.arguments().get("packageFilter");
                                boolean breakingOnly = Boolean.TRUE.equals(request.arguments().get("breakingOnly"));
                                JarApiDiffResult diff = DECOMPILER.compareJarApis(jar1, jar2, packageFilter, breakingOnly);
                                return McpSchema.CallToolResult.builder()
                                        .content(List.of(new McpSchema.TextContent(diff.formattedReport())))
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
                                .name("inspect_manifest")
                                .description("Inspect structured packaging metadata, MANIFEST.MF attributes, and SPI services of an archive")
                                .inputSchema(Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "jarPath", Map.of("type", "string", "description", "Path to .jar or .zip archive or Maven GAV coordinate")
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
                                ManifestSummary summary = DECOMPILER.inspectManifest(jar);
                                return McpSchema.CallToolResult.builder()
                                        .content(List.of(new McpSchema.TextContent(summary.formattedSummary())))
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
                                                "pathFilter", Map.of("type", "string", "description", "Optional package or path filter (e.g. com/example/service)"),
                                                "deobfuscate", Map.of("type", "boolean", "description", "Optional flag to rename illegal or obfuscated identifiers"),
                                                "showHexLiterals", Map.of("type", "boolean", "description", "Optional flag to format integer/bitmask constants as hexadecimal")
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
                                boolean deobfuscate = Boolean.TRUE.equals(request.arguments().get("deobfuscate"));
                                boolean showHex = Boolean.TRUE.equals(request.arguments().get("showHexLiterals"));
                                DecompileOptions options = DecompileOptions.builder()
                                        .deobfuscate(deobfuscate)
                                        .showHexLiterals(showHex)
                                        .build();
                                ExportResult result = DECOMPILER.exportArchive(jar, outDir, pathFilter, options);
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
