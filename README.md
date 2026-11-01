# javadec

> **Universal & Token-Efficient Java Decompiler and Bytecode Inspector for AI & Developers**

`javadec` is a modern Java Bytecode, Resource, and Archive inspector designed specifically for **AI Coding Agents** (Kiro, Claude, Cursor) and human developers. It features progressive hierarchical inspection, token-efficient skeletons (class outlining), zero-temp-file resource reading, and full/selective method decompilation.

---

## 🚀 Key Capabilities for AI & Developers

- **🌳 Archive Hierarchy & Tree View (`tree`)**: Visualize the whole package and resource structure in one compact view.
- **📄 Instant Resource Inspection (`resource`)**: Read `.properties`, `.yml`, `.xml`, `MANIFEST.MF`, `.sql`, etc., directly from memory without extracting the archive to disk.
- **⚡ Token-Efficient Skeleton Outlines (`outline`)**: Extract signatures, annotations, and fields, saving ~90% context tokens compared to dumping full source code.
- **🎯 Selective Method Decompilation (`view -m`)**: Decompile only the specific method you need to inspect.
- **📦 On-Demand Archive Decompilation (`view`)**: Decompile classes directly via archive URI (`library.jar!pkg/Class.class`) with automated inner class resolution.
- **📂 Archive Extraction (`extract`)**: Extract entire archives or targeted files to disk.
- **🤖 Built-in MCP Server (`javadec mcp`)**: Powered by the official Model Context Protocol Java SDK (`io.modelcontextprotocol.sdk`) for standard stdio integration.

---

## 📦 Quick Start via CLI (`npx`)

Requires Java runtime (JDK 21+):

```bash
# 1. View compact tree overview of classes and resources
npx javadec tree my-library.jar

# 2. Read manifest or configuration files
npx javadec resource my-library.jar META-INF/MANIFEST.MF
npx javadec resource my-app.jar application.yml

# 3. Find classes inside an archive (supports both package and path format)
npx javadec list my-library.jar -p com.example.service

# 4. Extract class outline / skeleton (save context tokens)
npx javadec outline my-library.jar!com/example/UserService.class

# 5. Decompile only a specific method
npx javadec view my-library.jar!com/example/UserService.class -m findById

# 6. Decompile full class
npx javadec view my-library.jar!com/example/UserService.class

# 7. Extract archive files to disk
npx javadec extract my-library.jar -o ./output -p META-INF/
```

---

## 🤖 Model Context Protocol (MCP) Integration

Configure `javadec` in your MCP client settings (e.g. `~/.kiro/settings/mcp.json` or Claude Desktop):

```json
{
  "mcpServers": {
    "javadec": {
      "command": "npx",
      "args": ["-y", "javadec", "mcp"],
      "env": {
        "JAVADEC_JAVA_PATH": "path/to/jdk-21/bin/java"
      }
    }
  }
}
```

### ⚙️ Environment Variables (Custom Java Runtime)

If your default system `java` is an older version (e.g. Java 8 or 11), you can specify a JDK 21+ runtime specifically for `javadec` without altering your system-wide `JAVA_HOME` or `PATH`:

| Variable | Description | Example |
|---|---|---|
| `JAVADEC_JAVA_PATH` | Direct path to the Java executable (highest precedence) | `C:\JDK\jdk-21\bin\java.exe` or `/usr/lib/jvm/jdk-21/bin/java` |
| `JAVADEC_JAVA_HOME` | Path to the JDK root folder | `C:\JDK\jdk-21` or `/usr/lib/jvm/jdk-21` |
| `JAVA_HOME` | Standard system Java Home (fallback) | `C:\Program Files\Java\jdk-21` |

### Available MCP Tools:
- `inspect_archive_tree(jarPath)`: Get archive hierarchy tree, class count, and key configuration files.
- `read_jar_resource(jarPath, resourcePath)`: Read configuration files (.yml, .properties, .xml, MANIFEST.MF) directly from memory.
- `list_jar_classes(jarPath, pattern?)`: Search class names and packages in archive.
- `get_class_outline(target)`: Extract class signatures/skeleton (saving ~90% tokens).
- `decompile_class(target)`: Decompile full class source code cleanly.
- `decompile_method(target, methodName)`: Decompile only a single method on demand.
- `extract_archive(jarPath, outputDir, pathFilter?)`: Extract files from archive to disk.

---

## 🛠 Local Development & Build

```bash
# Build Java modules with Maven Wrapper
./mvnw clean package      # Linux/macOS
.\mvnw.cmd clean package  # Windows

# Or build via npm
npm run build

# Publish locally to global npm for testing
npm run publish:local

# Publish locally skipping maven build (if already compiled)
npm run publish:local -- --skip-build

# Bump version and synchronize across package.json and all Maven pom.xml files
npm run bump patch    # 1.0.0 -> 1.0.1
npm run bump minor    # 1.0.0 -> 1.1.0
npm run bump 1.2.0    # Set explicit version
# Or use standard npm version (auto triggers pom sync, git commit & git tag)
npm version patch
```

---

## 📄 License
Apache-2.0
