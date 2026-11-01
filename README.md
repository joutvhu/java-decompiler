# javadec

> **Universal & Token-Efficient Java Decompiler and Bytecode Inspector for AI & Developers**

`javadec` is a modern Java Bytecode, Resource, and Archive inspector designed specifically for **AI Coding Agents** (Kiro, Claude, Cursor) and human developers. It features progressive hierarchical inspection, token-efficient skeletons (class outlining), zero-temp-file resource reading, customizable tree exploration (path & depth limits), line-based pagination (offset & limit), full/selective method decompilation, and full archive export into `.java` files on disk.

---

## 🚀 Key Capabilities for AI & Developers

- **🌳 Scoped Tree Hierarchy (`tree --path --depth`)**: Zoom in on specific packages/folders with configurable depth limit to prevent context bloating.
- **📄 Paginated Resource Reading (`resource --offset --limit`)**: Read `.properties`, `.yml`, `.xml`, `MANIFEST.MF`, `.sql`, etc., in chunks directly from memory.
- **⚡ Token-Efficient Skeleton Outlines (`outline`)**: Extract signatures, annotations, and fields, saving ~90% context tokens compared to dumping full source code.
- **🎯 Selective Method Decompilation (`view -m`)**: Decompile only the specific method you need to inspect (including all overloads and annotations).
- **📖 Chunked/Paginated Class View (`view --offset --limit`)**: Read massive classes line-by-line without hitting LLM context limits.
- **💾 Full Archive Source Export (`dump` / `export`)**: Decompile entire archives or specific packages into `.java` files on disk, copying resources while maintaining package structures.
- **📦 On-Demand Archive Decompilation (`view`)**: Decompile classes directly via archive URI (`library.jar!pkg/Class.class`) with automated inner class resolution.
- **📂 Raw Archive Extraction (`extract`)**: Extract raw archive files (bytecode/resources) to disk.
- **🤖 Built-in MCP Server (`javadec mcp`)**: Powered by the official Model Context Protocol Java SDK (`io.modelcontextprotocol.sdk`) for standard stdio integration.

---

## 📦 Quick Start via CLI (`npx`)

Requires Java runtime (JDK 21+):

```bash
# 1. View scoped tree hierarchy (e.g. only META-INF up to 2 levels deep)
npx javadec tree my-library.jar --path META-INF --depth 2

# 2. Read configuration files with line pagination (lines 1 to 50)
npx javadec resource my-library.jar application.yml --offset 1 --limit 50

# 3. Find classes inside an archive (supports both package and path format)
npx javadec list my-library.jar -p com.example.service

# 4. Extract class outline / skeleton (save ~90% context tokens)
npx javadec outline my-library.jar!com/example/UserService.class

# 5. Decompile only a specific method (all overloads)
npx javadec view my-library.jar!com/example/UserService.class -m findById

# 6. Decompile full class with pagination (lines 101 to 200)
npx javadec view my-library.jar!com/example/UserService.class --offset 101 --limit 100

# 7. Decompile & Export entire JAR to .java source files on disk
npx javadec dump my-library.jar -o ./src/decompiled -p com/example/service

# 8. Extract raw archive files as-is
npx javadec extract my-library.jar -o ./raw-output -p META-INF/
```

---

## 🤖 Model Context Protocol (MCP) Integration

Configure `javadec` in your MCP client settings (e.g. `~/.kiro/settings/mcp.json` or Claude Desktop):

```json
{
  "mcpServers": {
    "javadec": {
      "command": "npx",
      "args": ["-y", "javadec", "mcp"]
    }
  }
}
```

### Available MCP Tools:
- `inspect_archive_tree(jarPath, path?, depth?)`: Get archive hierarchy tree with custom sub-path and depth limit.
- `read_jar_resource(jarPath, resourcePath, lineOffset?, lineLimit?)`: Read configuration files with line pagination.
- `list_jar_classes(jarPath, pattern?)`: Search class names and packages in archive.
- `get_class_outline(target)`: Extract class signatures/skeleton (saving ~90% tokens).
- `decompile_class(target, lineOffset?, lineLimit?)`: Decompile full class source code with optional line pagination.
- `decompile_method(target, methodName, lineOffset?, lineLimit?)`: Decompile single method with optional line pagination.
- `export_archive(jarPath, outputDir, pathFilter?)`: Decompile entire archive (or package) into `.java` source files on disk.
- `extract_archive(jarPath, outputDir, pathFilter?)`: Extract raw archive files to disk.

---

## 📄 License
Apache-2.0
