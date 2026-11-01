package com.joutvhu.javadec.mcp;

import com.joutvhu.javadec.core.api.DecompileOptions;
import com.joutvhu.javadec.core.api.JavaDecompiler;
import com.joutvhu.javadec.core.model.ArchiveTree;
import com.joutvhu.javadec.core.model.ClassOutline;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * Standard MCP Server implementation powered by official Model Context Protocol Java SDK
 * (io.modelcontextprotocol.sdk).
 */
public class JavadecMcpServer {

    private static final JavaDecompiler DECOMPILER = new JavaDecompiler();

    public static void main(String[] args) {
        StdioServerTransportProvider transportProvider =
                new StdioServerTransportProvider(McpJsonDefaults.getMapper());

        McpSyncServer server = McpServer.sync(transportProvider)
                .serverInfo("javadec-mcp", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .toolCall(
                        McpSchema.Tool.builder()
                                .name("inspect_archive_tree")
                                .description("Inspect archive structure, returning a concise hierarchical tree, total class count, and key configuration resources")
                                .inputSchema(Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "jarPath", Map.of("type", "string", "description", "Path to .jar or .zip archive")
                                        ),
                                        "required", List.of("jarPath")
                                ))
                                .build(),
                        (exchange, request) -> {
                            try {
                                String jarPath = (String) request.arguments().get("jarPath");
                                ArchiveTree tree = DECOMPILER.inspectArchiveTree(new File(jarPath));
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
                                                "jarPath", Map.of("type", "string", "description", "Path to .jar or .zip archive"),
                                                "pattern", Map.of("type", "string", "description", "Optional substring or package filter for class names")
                                        ),
                                        "required", List.of("jarPath")
                                ))
                                .build(),
                        (exchange, request) -> {
                            try {
                                String jarPath = (String) request.arguments().get("jarPath");
                                String pattern = (String) request.arguments().get("pattern");
                                List<String> list = DECOMPILER.listArchiveClasses(new File(jarPath), pattern);
                                return McpSchema.CallToolResult.builder()
                                        .content(List.of(new McpSchema.TextContent(String.join("\n", list))))
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
                                .description("Read text content of any resource/configuration file (e.g. .yml, .properties, .xml, MANIFEST.MF, .sql) directly from archive")
                                .inputSchema(Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "jarPath", Map.of("type", "string", "description", "Path to .jar or .zip archive"),
                                                "resourcePath", Map.of("type", "string", "description", "Relative resource path inside archive (e.g. META-INF/MANIFEST.MF, application.yml)")
                                        ),
                                        "required", List.of("jarPath", "resourcePath")
                                ))
                                .build(),
                        (exchange, request) -> {
                            try {
                                String jarPath = (String) request.arguments().get("jarPath");
                                String resourcePath = (String) request.arguments().get("resourcePath");
                                String content = DECOMPILER.readArchiveResource(new File(jarPath), resourcePath);
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
                                                "target", Map.of("type", "string", "description", "Path to .class file or <jarPath>!<classPath>")
                                        ),
                                        "required", List.of("target")
                                ))
                                .build(),
                        (exchange, request) -> {
                            try {
                                String target = (String) request.arguments().get("target");
                                ClassOutline outline;
                                if (target.contains("!")) {
                                    String[] parts = target.split("!", 2);
                                    outline = DECOMPILER.getArchiveClassOutline(new File(parts[0]), parts[1]);
                                } else {
                                    outline = DECOMPILER.getClassOutline(new File(target));
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
                                .description("Decompile full class source code from a .class file or <jarPath>!<classPath>")
                                .inputSchema(Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "target", Map.of("type", "string", "description", "Path to .class file or <jarPath>!<classPath>")
                                        ),
                                        "required", List.of("target")
                                ))
                                .build(),
                        (exchange, request) -> {
                            try {
                                String target = (String) request.arguments().get("target");
                                String result;
                                if (target.contains("!")) {
                                    String[] parts = target.split("!", 2);
                                    result = DECOMPILER.decompileArchiveClass(new File(parts[0]), parts[1], DecompileOptions.defaults());
                                } else {
                                    result = DECOMPILER.decompileFile(new File(target), DecompileOptions.defaults());
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
                                .description("Selectively decompile only a single method inside a class to optimize AI context tokens")
                                .inputSchema(Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "target", Map.of("type", "string", "description", "Path to .class file or <jarPath>!<classPath>"),
                                                "methodName", Map.of("type", "string", "description", "Name of the method to decompile")
                                        ),
                                        "required", List.of("target", "methodName")
                                ))
                                .build(),
                        (exchange, request) -> {
                            try {
                                String target = (String) request.arguments().get("target");
                                String methodName = (String) request.arguments().get("methodName");
                                String result;
                                if (target.contains("!")) {
                                    String[] parts = target.split("!", 2);
                                    result = DECOMPILER.decompileArchiveMethod(new File(parts[0]), parts[1], methodName, DecompileOptions.defaults());
                                } else {
                                    File f = new File(target);
                                    byte[] bytes = Files.readAllBytes(f.toPath());
                                    result = DECOMPILER.decompileMethod(f.getName(), bytes, methodName, DecompileOptions.defaults());
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
                                .name("extract_archive")
                                .description("Extract all or matching files from an archive to a destination directory on disk")
                                .inputSchema(Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "jarPath", Map.of("type", "string", "description", "Path to .jar or .zip archive"),
                                                "outputDir", Map.of("type", "string", "description", "Directory path on disk to extract files into"),
                                                "pathFilter", Map.of("type", "string", "description", "Optional path prefix or filename filter")
                                        ),
                                        "required", List.of("jarPath", "outputDir")
                                ))
                                .build(),
                        (exchange, request) -> {
                            try {
                                String jarPath = (String) request.arguments().get("jarPath");
                                String outputDir = (String) request.arguments().get("outputDir");
                                String pathFilter = (String) request.arguments().get("pathFilter");
                                int count = DECOMPILER.extractArchive(new File(jarPath), new File(outputDir), pathFilter);
                                return McpSchema.CallToolResult.builder()
                                        .content(List.of(new McpSchema.TextContent("Successfully extracted " + count + " file(s) to " + outputDir)))
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

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            server.close();
        }
    }
}
