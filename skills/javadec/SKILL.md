---
name: "javadec"
description: "Universal & Token-Efficient Java Bytecode and Archive Inspector. Use whenever inspecting, reverse-engineering, exploring, decompiling, or exporting Java JAR archives, WARs, or .class files, especially when tracing external library stack traces, finding method/class callers and references, inspecting third-party API signatures, reading archive resources (manifest, YAML, properties), or verifying compiled bytecode. Triggers on: decompile, decompiler, bytecode, inspect jar, jar file, class outline, class skeleton, javadec, fernflower, cfr, view method, extract jar, dump jar, export jar, javadoc, sources jar, find references, callers, find usages."
---

# Javadec - Java Bytecode & Archive Inspector for AI Agents

`javadec` allows AI agents to inspect compiled Java artifacts (`.jar`, `.war`, `.zip`, `.class`) on-demand with minimal context token consumption. It features automatic Companion Sources JAR resolution (reading original Javadocs and comments when available), compact tree views with customizable path/depth, zero-extraction resource reading with line pagination, class outlines (skeletons), selective method decompilation, and full archive export to `.java` files on disk.

---

## 1. Operating Strategy (Token-First Hierarchy)

Never decompile an entire JAR or huge class file without inspecting outlines or scoping the tree first. Follow this progressive hierarchy:

```
[Target Archive / Class]
       │
       ├──► 1. Scoped Archive Tree: inspect_archive_tree / javadec tree [--path <dir>] [--depth <d>]
       │       Targeted view of packages, class counts, and key configs
       │
       ├──► 2. Read Resource (Paginated): read_jar_resource / javadec resource [--offset <o>] [--limit <l>]
       │       Read MANIFEST.MF, application.yml, spring.factories directly from memory
       │
       ├──► 3. Class Outline: get_class_outline / javadec outline
       │       Read signatures, interfaces, fields, and annotations (~90% token savings)
       │
       ├──► 4. Find References & Callers: find_references / javadec find <target>
       │       Instantly scan method callers, field accesses, or class usages with zero decompilation
       │
       ├──► 5. Method View: decompile_method / javadec view -m <method> [--offset <o>] [--limit <l>]
       │       Extract target method implementation (with Javadoc if -sources.jar exists)
       │
       ├──► 6. Full Decompile (Paginated): decompile_class / javadec view [--offset <o>] [--limit <l>]
       │       Auto-resolves companion -sources.jar for Javadocs, or falls back to native decompiler
       │
       └──► 7. Export to Disk: export_archive / javadec dump -o <outputDir> [-p <filter>]
               Export all decompiled .java source files and resources into a directory on disk
```

---

## 2. Javadoc & Original Comments Resolution

Bytecode `.class` files do not store comments or Javadoc. However, `javadec` features **Sources-Aware Inspection**:
- When given a binary JAR (e.g. `~/.m2/.../spring-core-6.1.0.jar`), `javadec` automatically checks for a companion `spring-core-6.1.0-sources.jar` in the same directory (or Gradle cache).
- If present, `javadec` reads the original author's `.java` file directly $\rightarrow$ **100% of Javadocs and inline comments are preserved**!
- If absent, `javadec` falls back seamlessly to the native decompiler engine.

---

## 3. Tool Execution Modes

Determine which execution mode is available in the current session:

### Mode A: MCP Tools (Preferred)
When `javadec` MCP server is active in the environment, invoke MCP tools directly:
- `inspect_archive_tree(jarPath, path?, depth?)` — tree inspection with optional sub-path filter and depth limit
- `read_jar_resource(jarPath, resourcePath, lineOffset?, lineLimit?)` — resource reader with line pagination
- `list_jar_classes(jarPath, pattern?)` — search classes (supports `com.example` and `com/example`)
- `get_class_outline(target)` — lightweight skeleton/signatures (~90% token savings)
- `find_references(jarPath, target, limit?, pathFilter?)` — ultra-fast caller/usage scanner with line numbers
- `get_callees(target, methodName?, limit?)` — extract outgoing method calls made by a class or method
- `analyze_dependencies(target)` — categorize imported classes and libraries (JDK, internal, third-party)
- `decompile_method(target, methodName, lineOffset?, lineLimit?)` — decompile single method/all overloads (with Javadoc if available)
- `decompile_class(target, lineOffset?, lineLimit?)` — decompile full class with line pagination (with Javadoc if available)
- `disassemble_bytecode(target, methodName?, lineOffset?, lineLimit?)` — disassemble into readable JVM opcodes (iload, invokevirtual, tableswitch)
- `extract_jar_strings(jarPath, pathFilter?, textFilter?, minLength?, limit?)` — extract hardcoded URLs, SQL queries, tokens, error messages without decompilation
- `inspect_stack_line(target, lineNumber, contextLines?)` — extract focused snippet surrounding a stack trace line (~95% token savings)
- `jar_diff(jar1, jar2, packageFilter?, breakingOnly?)` — compare binary API differences and breaking changes between two JARs or versions
- `inspect_manifest(jarPath)` — structured packaging metadata, main-class, build JDK, and SPI services
- `inspect_module(jarPath)` — extract JPMS module declaration (requires, exports, opens, uses, provides)
- `inspect_hierarchy(target)` — traverse full inheritance chain (superclasses up to Object) and all implemented interfaces
- `verify_jar_signature(jarPath)` — verify digital signature, certificate chain, and file integrity of a JAR archive
- `security_audit(jarPath, minSeverity?, pathFilter?)` — audit archive or class bytecode for security vulnerabilities and risky patterns
- `inspect_metrics(target, methodName?)` — inspect JVM bytecode technical metrics (instruction count, maxStack, maxLocals, try-catch)
- `export_archive(jarPath, outputDir, pathFilter?)` — decompile entire archive into `.java` files on disk
- `extract_archive(jarPath, outputDir, pathFilter?)` — extract raw archive files (bytecode/resources) to disk

### Mode B: Shell CLI (Fallback)
If MCP tools are not registered, execute CLI commands in the terminal using `javadec` (or `npx -y javadec`):
- `javadec tree <jarPath> [--path <subPath>] [--depth <n>]`
- `javadec resource <jarPath> <resourcePath> [--offset <n>] [--limit <n>]`
- `javadec list <jarPath> [-p <pattern>] [-a <annotation>]`
- `javadec outline <target>`
- `javadec find <jarPath> <target> [-l <limit>] [-p <filter>]` (aliases: `refs`, `usages`)
- `javadec callees <target> [-m <method>] [-l <limit>]` (aliases: `calls`, `outgoing`)
- `javadec deps <target>` (aliases: `dependencies`, `imports`)
- `javadec strings <target> [-f <filter>] [--min-len <n>] [-l <limit>] [-p <filter>]` (alias: `constants`)
- `javadec trace <target> <lineNumber> [-C <context>]` (alias: `stack`)
- `javadec diff <jar1> <jar2> [-p <filter>] [--breaking-only]` (alias: `delta`)
- `javadec manifest <archiveOrGav>`
- `javadec module <archiveOrGav>` (aliases: `jpms`)
- `javadec hierarchy <target>` (aliases: `types`, `tree-class`)
- `javadec verify <archiveOrGav>` (aliases: `signature`)
- `javadec audit <archiveOrClass> [--min-severity <level>] [-p <filter>]` (aliases: `security`, `check`)
- `javadec metrics <target> [-m <method>]` (aliases: `stats`)
- `javadec view <target> -m <methodName> [--offset <n>] [--limit <n>] [--deobfuscate] [--hex] [-cp <path>] [--no-deps]`
- `javadec view <target> [--offset <n>] [--limit <n>] [--deobfuscate] [--hex] [-cp <path>] [--no-deps]`
- `javadec disasm <target> [-m <method>] [--offset <n>] [--limit <n>]` (aliases: `bytecode`, `ops`)
- `javadec dump <jarPath> -o <outputDir> [-p <filter>] [--deobfuscate] [--hex] [-cp <path>] [--no-deps]` (alias: `export`)
- `javadec extract <jarPath> -o <outputDir> [-p <filter>]`

*Note on Java Runtime: If the system default Java is older than Java 21, set `JAVADEC_JAVA_PATH` or `JAVADEC_JAVA_HOME` pointing to a JDK 21+ installation when executing CLI commands.*

---

## 4. Target URI Format

When addressing a class, use the standard target URI syntax:
- **Maven Coordinate (GAV)**: `<groupId>:<artifactId>:<version>!<internal/package/ClassName.class>`  
  *(Automatically checks `~/.m2/repository` and downloads from Maven Central if absent!)*  
  *Examples:*
  - `org.springframework:spring-core:6.1.0!org/springframework/core/io/Resource.class`
  - `com.google.guava:guava:33.0.0-jre!com/google/common/collect/ImmutableList.class`
- **Class inside JAR archive**: `<path/to/archive.jar>!<internal/package/ClassName.class>`  
  *Examples:*
  - `C:/Users/name/.m2/repository/org/springframework/spring-core/6.1.0/spring-core-6.1.0.jar!org/springframework/core/io/Resource.class`
  - `target/app.jar!com/example/service/OrderService.class`
- **Standalone `.class` file**: `<path/to/ClassName.class>`  
  *Example:* `target/classes/com/example/util/StringUtils.class`
- **Inner or Nested Classes**: Separate with `$`  
  *Example:* `app.jar!com/example/Map$Entry.class`

---

## 5. Workflows & Command Reference

### Workflow A: Explore an Unfamiliar JAR with Scoped Tree
1. **Get High-Level or Scoped Archive Tree**:
   - Entire archive: `inspect_archive_tree({ "jarPath": "lib.jar", "depth": 2 })`
   - Focused folder: `inspect_archive_tree({ "jarPath": "lib.jar", "path": "META-INF", "depth": 2 })`
   - CLI: `javadec tree lib.jar --path org/springframework --depth 2`

2. **Read Configurations & Metadata (Chunked/Paginated if Large)**:
   - MCP: `read_jar_resource({ "jarPath": "lib.jar", "resourcePath": "META-INF/MANIFEST.MF", "lineOffset": 1, "lineLimit": 50 })`
   - CLI: `javadec resource lib.jar application.yml --offset 1 --limit 50`

3. **Search for Specific Classes**:
   - MCP: `list_jar_classes({ "jarPath": "lib.jar", "pattern": "Security" })`
   - CLI: `javadec list lib.jar -p com.example.security`

---

### Workflow B: Investigate Stack Traces & Method Contracts (with Javadoc)
1. **Extract Class Outline (Skeleton)**:
   Always inspect outline first to verify field definitions and method signatures:
   - MCP: `get_class_outline({ "target": "lib.jar!com/example/AuthManager.class" })`
   - CLI: `javadec outline lib.jar!com/example/AuthManager.class`

2. **Inspect Target Method Implementation (with Original Javadoc if Sources JAR exists)**:
   Extract the exact logic and Javadoc documentation of the method:
   - MCP: `decompile_method({ "target": "lib.jar!com/example/AuthManager.class", "methodName": "authenticate" })`
   - CLI: `javadec view lib.jar!com/example/AuthManager.class -m authenticate`

3. **Paginated Decompilation for Large Classes**:
   If a 2000-line class must be audited, read in chunks to keep prompt context clean:
   - MCP: `decompile_class({ "target": "lib.jar!com/example/LargeService.class", "lineOffset": 1, "lineLimit": 100 })`
   - CLI: `javadec view lib.jar!com/example/LargeService.class --offset 101 --limit 100`

---

### Workflow C: Decompile & Export Entire Archive to Source Code (.java)
When the user requests to decompile an entire library or package into readable `.java` files for offline browsing or migration:
- MCP: `export_archive({ "jarPath": "lib.jar", "outputDir": "./src/decompiled", "pathFilter": "com/example/service" })`
- CLI: `javadec dump lib.jar -o ./src/decompiled -p com/example/service`

---

### Workflow D: Extract Raw Archive Resources or Bytecode
When files need to be unpacked to disk as-is:
- MCP: `extract_archive({ "jarPath": "lib.jar", "outputDir": "./extracted", "pathFilter": "META-INF/" })`
- CLI: `javadec extract lib.jar -o ./extracted -p META-INF/`

---

## 6. Locating Dependencies on the System

When searching for JAR files mentioned in project pom.xml or build.gradle:
- **Maven Local Repository**:
  `$HOME/.m2/repository/<groupId-as-path>/<artifactId>/<version>/<artifactId>-<version>.jar`  
  *(Example: `~/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.17.0/jackson-databind-2.17.0.jar`)*
- **Gradle Cache**:
  `$HOME/.gradle/caches/modules-2/files-2.1/<groupId>/<artifactId>/<version>/<hash>/...jar`
- **Project Outputs**:
  - Maven: `target/classes/`, `target/*.jar`
  - Gradle: `build/classes/java/main/`, `build/libs/*.jar`
