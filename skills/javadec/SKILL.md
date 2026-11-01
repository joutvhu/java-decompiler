---
name: "javadec"
description: "Universal & Token-Efficient Java Bytecode and Archive Inspector. Use whenever inspecting, reverse-engineering, exploring, or decompiling Java JAR archives, WARs, or .class files, especially when tracing external library stack traces, inspecting third-party API signatures, reading archive resources (manifest, YAML, properties), or verifying compiled bytecode. Triggers on: decompile, decompiler, bytecode, inspect jar, jar file, class outline, class skeleton, javadec, fernflower, cfr, view method, extract jar."
---

# Javadec - Java Bytecode & Archive Inspector for AI Agents

`javadec` allows AI agents to inspect compiled Java artifacts (`.jar`, `.war`, `.zip`, `.class`) on-demand with minimal context token consumption. It avoids dumping huge source files by offering compact tree views, zero-extraction resource reading, class outlines (skeletons), and selective method-level decompilation.

---

## 1. Operating Strategy (Token-First Hierarchy)

Never decompile an entire JAR or huge class file without inspecting outlines first. Follow this progressive hierarchy:

```
[Target Archive / Class]
       │
       ├──► 1. Archive Tree: inspect_archive_tree / javadec tree
       │       Quick overview of packages, class counts, and key configs
       │
       ├──► 2. Read Resource: read_jar_resource / javadec resource
       │       Read MANIFEST.MF, application.yml, spring.factories directly from memory
       │
       ├──► 3. Class Outline: get_class_outline / javadec outline
       │       Read signatures, interfaces, fields, and annotations (~90% token savings)
       │
       ├──► 4. Method View: decompile_method / javadec view -m <method>
       │       Decompile only the implementation logic of the target method
       │
       └──► 5. Full Decompile: decompile_class / javadec view
               Fallback only when cross-method flow or complete class structure is required
```

---

## 2. Tool Execution Modes

Determine which execution mode is available in the current session:

### Mode A: MCP Tools (Preferred)
When `javadec` MCP server is active in the environment, invoke MCP tools directly:
- `inspect_archive_tree(jarPath)`
- `read_jar_resource(jarPath, resourcePath)`
- `list_jar_classes(jarPath, pattern?)`
- `get_class_outline(target)`
- `decompile_method(target, methodName)`
- `decompile_class(target)`
- `extract_archive(jarPath, outputDir, pathFilter?)`

### Mode B: Shell CLI (Fallback)
If MCP tools are not registered, execute CLI commands in the terminal using `javadec` (or `npx -y javadec`):
- `javadec tree <jarPath>`
- `javadec resource <jarPath> <resourcePath>`
- `javadec list <jarPath> -p <pattern>`
- `javadec outline <target>`
- `javadec view <target> -m <methodName>`
- `javadec view <target>`
- `javadec extract <jarPath> -o <outputDir> [-p <filter>]`

*Note on Java Runtime: If the system default Java is older than Java 21, set `JAVADEC_JAVA_PATH` or `JAVADEC_JAVA_HOME` pointing to a JDK 21+ installation when executing CLI commands.*

---

## 3. Target URI Format

When addressing a class, use the standard target URI syntax:
- **Class inside JAR archive**: `<path/to/archive.jar>!<internal/package/ClassName.class>`  
  *Examples:*
  - `C:/Users/name/.m2/repository/org/springframework/spring-core/6.1.0/spring-core-6.1.0.jar!org/springframework/core/io/Resource.class`
  - `target/app.jar!com/example/service/OrderService.class`
- **Standalone `.class` file**: `<path/to/ClassName.class>`  
  *Example:* `target/classes/com/example/util/StringUtils.class`
- **Inner or Nested Classes**: Separate with `$`  
  *Example:* `app.jar!com/example/Map$Entry.class`

---

## 4. Workflows & Command Reference

### Workflow A: Explore an Unfamiliar JAR
1. **Get Archive Tree Overview**:
   - MCP: `inspect_archive_tree({ "jarPath": "path/to/lib.jar" })`
   - CLI: `javadec tree path/to/lib.jar`
   - *Result*: Summarizes total class count, top-level package hierarchy, and list of configuration files.

2. **Read Configurations & Metadata**:
   - MCP: `read_jar_resource({ "jarPath": "path/to/lib.jar", "resourcePath": "META-INF/MANIFEST.MF" })`
   - CLI: `javadec resource path/to/lib.jar META-INF/MANIFEST.MF`
   - *Tip*: Also inspect `application.yml`, `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, etc.

3. **Search for Specific Classes**:
   - MCP: `list_jar_classes({ "jarPath": "path/to/lib.jar", "pattern": "Security" })`
   - CLI: `javadec list path/to/lib.jar -p Security`
   - *Note*: Pattern supports both dot (`com.example`) and slash (`com/example`) notation.

---

### Workflow B: Investigate Stack Traces & Method Contracts
When debugging an exception that originates from an external library:

1. **Extract Class Outline (Skeleton)**:
   Always run outline first to verify field definitions and method signatures without bodies:
   - MCP: `get_class_outline({ "target": "lib.jar!com/example/AuthManager.class" })`
   - CLI: `javadec outline lib.jar!com/example/AuthManager.class`
   - *Evaluation*: Inspect method parameter types, annotations (`@Transactional`, `@Nullable`), and return types.

2. **Decompile Only the Problematic Method**:
   Extract the exact logic of the method identified from the stack trace:
   - MCP: `decompile_method({ "target": "lib.jar!com/example/AuthManager.class", "methodName": "authenticate" })`
   - CLI: `javadec view lib.jar!com/example/AuthManager.class -m authenticate`

3. **Full Decompilation (Only If Method View is Insufficient)**:
   If variable initialization, static blocks, or inner class callbacks need to be traced:
   - MCP: `decompile_class({ "target": "lib.jar!com/example/AuthManager.class" })`
   - CLI: `javadec view lib.jar!com/example/AuthManager.class`

---

### Workflow C: Extract Targeted Resources or Code
When files need to be unpacked to disk for building, referencing, or analyzing:
- MCP: `extract_archive({ "jarPath": "lib.jar", "outputDir": "./extracted", "pathFilter": "META-INF/" })`
- CLI: `javadec extract lib.jar -o ./extracted -p META-INF/`

---

## 5. Locating Dependencies on the System

When searching for JAR files mentioned in project pom.xml or build.gradle:
- **Maven Local Repository**:
  `$HOME/.m2/repository/<groupId-as-path>/<artifactId>/<version>/<artifactId>-<version>.jar`  
  *(Example: `~/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.17.0/jackson-databind-2.17.0.jar`)*
- **Gradle Cache**:
  `$HOME/.gradle/caches/modules-2/files-2.1/<groupId>/<artifactId>/<version>/<hash>/...jar`
- **Project Outputs**:
  - Maven: `target/classes/`, `target/*.jar`
  - Gradle: `build/classes/java/main/`, `build/libs/*.jar`
