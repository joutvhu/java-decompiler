# Java Decompiler (javadec)

<p align="center">
  <strong>Universal &amp; Token-Efficient Java Bytecode, Resource, and Archive Inspector for AI &amp; Developers</strong>
</p>

<p align="center">
  <a href="https://www.npmjs.com/package/javadec"><img src="https://img.shields.io/npm/v/javadec.svg?style=flat-square" alt="npm version" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-Apache--2.0-blue.svg?style=flat-square" alt="License" /></a>
  <a href="https://openjdk.org/"><img src="https://img.shields.io/badge/Java-21%2B-orange.svg?style=flat-square" alt="Java 21+" /></a>
  <a href="https://nodejs.org/"><img src="https://img.shields.io/badge/Node.js-%3E%3D16-green.svg?style=flat-square" alt="Node.js >= 16" /></a>
  <a href="https://modelcontextprotocol.io/"><img src="https://img.shields.io/badge/MCP-Compatible-purple.svg?style=flat-square" alt="MCP Compatible" /></a>
</p>

---

## 💡 Why javadec?

Traditional Java decompilers (like JD-GUI, standalone Fernflower, or CLI CFR) are built for manual human inspection: they dump entire archives to disk, emit thousands of lines of code into a single window, and lose author documentation.

When used with **AI Coding Agents** (such as Kiro, Claude, Cursor, Copilot Workspace) or modern CLI workflows, traditional approaches hit three fatal bottlenecks:
1. **Context Window Exhaustion**: Dumping an entire class (or JAR) burns 5,000–30,000 context tokens in a single prompt.
2. **Lost Javadocs & Comments**: Bytecode (`.class`) does not retain comments or Javadoc. Decompilers output bare code stripped of documentation.
3. **Disk Clutter & Incompatible Archives**: Unpacking entire JARs generates temporary file clutter on disk, and traditional tools fail on Spring Boot executable JARs (`BOOT-INF/classes`) or WAR files (`WEB-INF/classes`).

**`javadec` solves all of these problems with a progressive, token-first inspection hierarchy:**

```
[Target Archive / Class]
       │
       ├──► 1. Scoped Archive Tree:    javadec tree [--path <dir>] [--depth <d>]
       │       High-level summary of packages, class counts, and key configs
       │
       ├──► 2. Read Resource (Paged):  javadec resource [--offset <o>] [--limit <l>]
       │       Read MANIFEST.MF, application.yml, spring.factories directly from memory
       │
       ├──► 3. Class Outline:          javadec outline
       │       Lightweight skeleton (signatures, fields, annotations) — ~90% token savings
       │
       ├──► 4. Find Callers & Usages:  javadec find <target>
       │       Scan method callers, field accesses, or class usages with line numbers in ms
       │
       ├──► 5. Method View:            javadec view -m <method> [--offset <o>] [--limit <l>]
       │       Decompile only the target method (with Javadoc if sources jar exists)
       │
       ├──► 6. Full Decompile (Paged): javadec view [--offset <o>] [--limit <l>]
       │       Auto-resolves companion -sources.jar for Javadoc, or falls back to native decompiler
       │
       └──► 7. Export to Disk:         javadec dump -o <outputDir> [-p <filter>]
               Decompile entire archive or package into clean .java files on disk
```

---

## ✨ Key Features

- **📖 Sources-Aware Inspection (Javadoc Preservation)**: Automatically locates companion `<artifact>-<version>-sources.jar` in Maven local repository (`~/.m2/repository`) or Gradle cache (`~/.gradle/caches`). When present, it serves original author source code with **100% of Javadocs and inline comments intact**!
- **🔗 Automatic Dependency & Classpath Resolution**: Automatically identifies external dependencies from companion `.pom` files in `~/.m2/repository`, embedded `pom.xml` inside JARs, and `BOOT-INF/lib`. Supports manual `-cp, --classpath` with directories or Maven GAVs for 100% type-system precision.
- **📦 Direct Maven Coordinate Resolution (`g:a:v`)**: Inspect libraries directly by Maven coordinates (e.g. `org.springframework:spring-core:6.1.0`), checking `~/.m2/repository` and auto-downloading from Maven Central on-demand.
- **⚡ Token-Efficient Skeletons via ASM (`outline`)**: Extracts full class signatures, fields, annotations, constructors, and parameter names in ~100–300 tokens without decompiling method bodies.
- **🔍 Instant Bytecode Reference & Caller Search (`find` / `refs`)**: Scans thousands of classes in milliseconds directly in bytecode to locate all callers of a method, field reads/writes, or class instantiations with line numbers.
- **⚙️ JVM Bytecode Disassembly (`disasm`)**: Low-level opcode and instruction disassembler (`iload`, `invokevirtual`, `monitorenter`) for inspecting obfuscated or corrupted bytecode.
- **🛡️ Deobfuscation & Hex Literals (`--deobfuscate`, `--hex`)**: Automatically cleans up illegal/obfuscated identifier names and formats bitmasks as hexadecimal literals.
- **🎯 Precision Method Decompilation (`view -m`)**: Decompiles only the targeted method (handling all overloads, constructor initializers, abstract/interface methods, and preceding annotations).
- **📑 Line-Based Pagination (`--offset` & `--limit`)**: Read massive classes or resources in bite-sized chunks to prevent prompt context bloat.
- **🌳 Scoped Tree Hierarchy (`tree --path --depth`)**: Zoom in on specific packages or sub-directories with configurable depth limit.
- **🍃 Spring Boot & WAR Native**: Transparently resolves classes and resources inside `BOOT-INF/classes/` and `WEB-INF/classes/`.
- **💾 Full Archive Source Export (`dump` / `export`)**: Decompiles entire JARs into browsable `.java` source files on disk, copying resources while maintaining package structures.
- **🤖 Built-in MCP Server (`javadec mcp`)**: Powered by the official Model Context Protocol Java SDK (`io.modelcontextprotocol.sdk`) over stdio.
- **🔒 Zero-Temp-File & Zip-Slip Protected**: All on-demand inspections happen purely in-memory; disk export is strictly sanitized against path traversal vulnerabilities.

---

## 🛠️ Prerequisites & Installation

### Prerequisites
- **Node.js**: Version 16.0.0 or higher.
- **Java Runtime**: JDK 21 or higher.

*Note on Java Detection: `javadec` automatically checks `JAVADEC_JAVA_PATH`, `JAVADEC_JAVA_HOME`, `JAVA_HOME`, and system `PATH`. If your system default Java is older (e.g. Java 8), simply set `JAVADEC_JAVA_HOME` pointing to JDK 21+.*

### Installation Options

#### Option 1: Run instantly via `npx` (No installation needed)
```bash
npx javadec --help
```

#### Option 2: Install globally via npm
```bash
npm install -g javadec
javadec --help
```

---

## 💻 CLI Command Reference & Examples

### 1. Inspect Archive Tree (`tree`)
Visualizes the archive hierarchy, class counts, and configuration files with customizable depth and path filters:

```bash
# Overview of the entire archive (default depth: 3)
javadec tree library.jar

# Scope to a specific sub-directory with depth 2
javadec tree library.jar --path META-INF --depth 2
javadec tree app.jar -p org/springframework -d 3
```

*Output Example:*
```
library.jar!META-INF (2.2 MB, 0 classes, 4 resources, depth=2)
├── intellij.java.decompiler.kotlin_module (119 B)
├── plugin.xml (2.5 KB)
├── pluginIcon.svg (2.1 KB)
└── pluginIcon_dark.svg (2.1 KB)
```

---

### 2. Read Configuration & Resource Files (`resource`)
Reads text content of non-class configuration files (`application.yml`, `.properties`, `.xml`, `MANIFEST.MF`, `.sql`, etc.) directly from archive memory:

```bash
# Read entire manifest
javadec resource library.jar META-INF/MANIFEST.MF

# Read Spring Boot configuration with line pagination (lines 1 to 50)
javadec resource app.jar application.yml --offset 1 --limit 50
```

---

### 3. List Classes in Archive (`list`)
Searches class entries inside the archive using substring or package matching:

```bash
# List all classes
javadec list library.jar

# Search by package or class name (supports both dot and slash notation)
javadec list library.jar -p com.example.service
javadec list library.jar -p Security
```

---

### 4. Extract Class Outline / Skeleton (`outline`)
Extracts class signatures, interfaces, fields, annotations, and authentic parameter names without method bodies (~90% token savings):

```bash
# Using URI syntax
javadec outline library.jar!com/example/service/OrderService.class

# Using space-separated arguments
javadec outline library.jar com/example/service/OrderService.class

# Standalone .class file on disk
javadec outline target/classes/com/example/util/StringUtils.class
```

*Output Example:*
```java
package com.example.service;

@Service
@Transactional
public class OrderService implements IOrderService {
    // Fields
    @Autowired
    private OrderRepository orderRepository;

    // Methods
    public OrderService(OrderRepository orderRepository);
    public Order createOrder(String customerId, List<Item> items) throws OrderException;
    public boolean cancelOrder(String orderId);
}
```

---

### 5. Decompile Class or Single Method (`view`)
Decompiles full class source code or selectively targets a single method. If a companion `-sources.jar` exists alongside the binary JAR, original Javadocs and comments are automatically preserved:

```bash
# Decompile only a specific method (includes all overloads, annotations & Javadoc)
javadec view library.jar!com/example/service/OrderService.class -m createOrder

# Decompile full class
javadec view library.jar!com/example/service/OrderService.class

# Read large decompiled class in chunks (lines 101 to 200)
javadec view library.jar!com/example/service/OrderService.class --offset 101 --limit 100

# Include bytecode line number comments
javadec view library.jar!com/example/service/OrderService.class --lines
```

---

### 6. Decompile & Export Entire Archive (`dump` / `export`)
Decompiles all classes (or a filtered package) into `.java` source files on disk, copying resources while maintaining package structures:

```bash
# Decompile entire JAR into a directory
javadec dump library.jar -o ./decompiled-src

# Decompile only classes matching a package filter
javadec dump library.jar -o ./decompiled-src -p com/example/service

# Include line number comments in exported files
javadec dump library.jar -o ./decompiled-src --lines
```

---

### 7. Extract Raw Archive Files (`extract`)
Unpacks raw archive entries (bytecode `.class` and resources) directly to disk:

```bash
# Extract all files
javadec extract library.jar -o ./raw-extracted

# Extract only META-INF resources
javadec extract library.jar -o ./raw-extracted -p META-INF/
```

---

## 🤖 Model Context Protocol (MCP) Server

`javadec` includes a native Model Context Protocol (MCP) server powered by the official Java SDK (`io.modelcontextprotocol.sdk`) communicating over stdio JSON-RPC.

### Client Configuration

Add `javadec` to your MCP client configuration:

#### Kiro IDE (`~/.kiro/settings/mcp.json` or `<workspace>/.kiro/settings/mcp.json`):
```json
{
  "mcpServers": {
    "javadec": {
      "command": "npx",
      "args": ["-y", "javadec", "mcp"],
      "disabled": false
    }
  }
}
```

#### Claude Desktop (`claude_desktop_config.json`):
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

#### Custom JDK Configuration:
If your default Java is older than JDK 21, specify `JAVADEC_JAVA_HOME` in the `env` block:
```json
{
  "mcpServers": {
    "javadec": {
      "command": "npx",
      "args": ["-y", "javadec", "mcp"],
      "env": {
        "JAVADEC_JAVA_HOME": "C:\\Program Files\\Java\\jdk-21"
      }
    }
  }
}
```

---

### MCP Tool Catalog

| Tool Name | Parameters | Description |
| :--- | :--- | :--- |
| **`inspect_archive_tree`** | `jarPath` (req), `path` (opt), `depth` (opt) | Returns a compact tree structure with total class/resource counts. |
| **`read_jar_resource`** | `jarPath` (req), `resourcePath` (req), `lineOffset` (opt), `lineLimit` (opt) | Reads non-class text resources (`.yml`, `.properties`, `.xml`, `MANIFEST.MF`) with pagination. |
| **`list_jar_classes`** | `jarPath` (req), `pattern` (opt) | Lists classes matching a package or substring pattern. |
| **`get_class_outline`** | `target` (req), or (`jarPath` + `classPath`) | Extracts class skeleton/signatures without method bodies (~90% token savings). |
| **`find_references`** | `jarPath` (req), `target` (req), `limit` (opt), `pathFilter` (opt) | Finds all callers, field accesses, or class usages directly from bytecode with line numbers. |
| **`get_callees`** | `target` (req), `methodName` (opt), `limit` (opt) | Extracts outgoing method calls (callees) called from within a class or method. |
| **`analyze_dependencies`** | `target` (req) | Analyzes and categorizes imported classes and packages (JDK, internal, third-party). |
| **`decompile_class`** | `target` (req), `classpath` (opt), `autoDetectDependencies` (opt), `lineOffset` (opt), `lineLimit` (opt) | Decompiles full class source code (auto-resolves `-sources.jar` for Javadoc and external dependencies). |
| **`decompile_method`** | `target` (req), `methodName` (req), `classpath` (opt), `lineOffset` (opt), `lineLimit` (opt) | Decompiles only a single method and its overloads (with Javadoc if available). |
| **`disassemble_bytecode`** | `target` (req), `methodName` (opt), `lineOffset` (opt), `lineLimit` (opt) | Disassembles bytecode into human-readable JVM instructions and opcodes. |
| **`extract_jar_strings`** | `jarPath` (req), `pathFilter` (opt), `textFilter` (opt), `minLength` (opt), `limit` (opt) | Extracts hardcoded string constants (URLs, SQL, tokens, error messages) without decompilation. |
| **`inspect_stack_line`** | `target` (req), `lineNumber` (req), `contextLines` (opt) | Locates and extracts source snippet surrounding an exception stack trace line (~95% token savings). |
| **`jar_diff`** | `jar1` (req), `jar2` (req), `packageFilter` (opt), `breakingOnly` (opt) | Compares binary public API differences and breaking changes between two JARs or versions. |
| **`inspect_manifest`** | `jarPath` (req) | Inspects structured packaging metadata, MANIFEST.MF attributes, and SPI services. |
| **`inspect_module`** | `jarPath` (req) | Extracts JPMS module declaration (requires, exports, opens, uses, provides) from an archive. |
| **`inspect_hierarchy`** | `target` (req) | Traverses and extracts the full inheritance chain (superclasses up to Object) and all implemented interfaces. |
| **`verify_jar_signature`** | `jarPath` (req) | Verifies digital signature, certificate chain (Issuer/Subject/Validity), and file integrity of a JAR archive. |
| **`security_audit`** | `jarPath` (req), `minSeverity` (opt), `pathFilter` (opt) | Audits bytecode for security vulnerabilities (command injection, deserialization, reflection bypass, weak crypto). |
| **`inspect_metrics`** | `target` (req), `methodName` (opt) | Inspects JVM technical bytecode metrics (instructions count, maxStack, maxLocals, tryCatch blocks). |
| **`export_archive`** | `jarPath` (req), `outputDir` (req), `pathFilter` (opt), `classpath` (opt) | Decompiles entire archive or package into `.java` files on disk. |
| **`extract_archive`** | `jarPath` (req), `outputDir` (req), `pathFilter` (opt) | Extracts raw archive files (bytecode and resources) to disk as-is. |

---

## 🎯 Recommended AI Agent Workflow

When integrating `javadec` with AI assistants (via MCP or CLI), follow this optimal token-conserving procedure:

### Scenario: Investigating an Exception in a Third-Party Library
1. **Locate JAR in Dependency Cache**:
   Find the library in `~/.m2/repository` or `~/.gradle/caches`.
2. **Inspect Outline First**:
   Call `get_class_outline("lib.jar!com/example/AuthManager.class")` to verify class fields, constructors, and method signatures (~200 tokens).
3. **Decompile the Faulty Method**:
   Call `decompile_method("lib.jar!com/example/AuthManager.class", "authenticate")` to read the exact logic and Javadoc of the method from the stack trace (~300 tokens).
4. **Check Configuration (If needed)**:
   Call `read_jar_resource("lib.jar", "META-INF/spring.factories")` to see how components are auto-configured.

Total tokens consumed: **~700 tokens** (compared to **15,000+ tokens** if dumping entire files).

---

## 🏗️ Architecture & Building from Source

`javadec` is structured as a Maven multi-module project packaged with a Node.js npm CLI wrapper:

```
java-decompiler/
├── pom.xml                               # Parent Maven POM
├── package.json                          # NPM Package configuration
├── bin/
│   └── javadec.js                        # Node.js launcher (dispatches to CLI or MCP jar)
├── jars/
│   ├── javadec-cli.jar                   # Shaded fat-jar for CLI
│   └── javadec-mcp.jar                   # Shaded fat-jar for MCP Server
├── javadec-core/                         # Core library
│   ├── com.joutvhu.javadec.core.*        # Loader, Outline, Model, Decompiler Facade
│   └── com.joutvhu.javadec.engine.*      # Native Decompiler Engine (Java 8 - 27+)
├── javadec-cli/                          # Command Line Interface (Picocli)
├── javadec-mcp/                          # Model Context Protocol Server (MCP Java SDK)
├── scripts/
│   ├── build.js                          # Build automation script
│   └── bump-version.js                   # Semver bump utility
└── skills/
    └── javadec/SKILL.md                  # Comprehensive AI Agent guidance skill
```

### Build Commands

```bash
# Build all modules and package JARs into jars/
npm run build

# Run unit tests
mvn test

# Test CLI locally
node bin/javadec.js --help

# Test MCP Server locally
node bin/javadec.js mcp

# Bump version across package.json, POMs, and Java classes
npm run bump:patch       # 1.0.0 -> 1.0.1
npm run bump:minor       # 1.0.0 -> 1.1.0
npm run bump:major       # 1.0.0 -> 2.0.0
npm run bump:prerelease  # 1.0.0 -> 1.0.1-0
```

---

## 📄 License

Licensed under the **Apache License, Version 2.0**. See the [LICENSE](LICENSE) file for details.
