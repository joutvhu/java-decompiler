package com.joutvhu.javadec.mcp;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JavadecMcpServerTest {

    private static File testJar;

    private static synchronized File getTestJar() {
        if (testJar != null && testJar.exists()) {
            return testJar;
        }

        try {
            File temp = File.createTempFile("javadec-mcp-test-", ".jar");
            temp.deleteOnExit();

            String source = """
                    package com.joutvhu.javadec.test;
                    public class SampleService {
                        public static void main(String[] args) {
                            System.out.println("SampleService MCP mock");
                        }
                    }
                    """;
            javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
            File srcDir = java.nio.file.Files.createTempDirectory("javadec-mcp-src-").toFile();
            File clsDir = java.nio.file.Files.createTempDirectory("javadec-mcp-cls-").toFile();
            File javaFile = new File(srcDir, "com/joutvhu/javadec/test/SampleService.java");
            javaFile.getParentFile().mkdirs();
            clsDir.mkdirs();
            java.nio.file.Files.writeString(javaFile.toPath(), source);

            compiler.run(null, null, null, "-parameters", "-g", "-d", clsDir.getAbsolutePath(), javaFile.getAbsolutePath());

            File classFile = new File(clsDir, "com/joutvhu/javadec/test/SampleService.class");
            byte[] classBytes = java.nio.file.Files.readAllBytes(classFile.toPath());

            try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(temp))) {
                zos.putNextEntry(new java.util.zip.ZipEntry("com/joutvhu/javadec/test/SampleService.class"));
                zos.write(classBytes);
                zos.closeEntry();
            }

            java.nio.file.Files.walk(srcDir.toPath()).sorted(java.util.Comparator.reverseOrder()).map(java.nio.file.Path::toFile).forEach(File::delete);
            java.nio.file.Files.walk(clsDir.toPath()).sorted(java.util.Comparator.reverseOrder()).map(java.nio.file.Path::toFile).forEach(File::delete);

            testJar = temp;
            return testJar;
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare test JAR", e);
        }
    }

    private final File refJar = getTestJar();

    @Test
    public void testIsArchiveFile() {
        assertTrue(JavadecMcpServer.isArchiveFile("lib.jar"));
        assertTrue(JavadecMcpServer.isArchiveFile("archive.zip"));
        assertTrue(JavadecMcpServer.isArchiveFile("app.war"));
        assertTrue(JavadecMcpServer.isArchiveFile("enterprise.ear"));
        assertFalse(JavadecMcpServer.isArchiveFile("MyClass.class"));
        assertFalse(JavadecMcpServer.isArchiveFile("MyClass.java"));
        assertFalse(JavadecMcpServer.isArchiveFile(null));
    }

    @Test
    public void testResolveTargetWithVariousFormats() {
        // 1. Direct target URI
        String t1 = JavadecMcpServer.resolveTarget(Map.of("target", "lib.jar!com/example/MyClass.class"));
        assertEquals("lib.jar!com/example/MyClass.class", t1);

        // 2. Separate jarPath and classPath
        String t2 = JavadecMcpServer.resolveTarget(Map.of("jarPath", "lib.jar", "classPath", "com/example/MyClass.class"));
        assertEquals("lib.jar!com/example/MyClass.class", t2);

        // 3. Quoted path stripping
        String t3 = JavadecMcpServer.resolveTarget(Map.of("target", "\"lib.jar!com/example/MyClass.class\""));
        assertEquals("lib.jar!com/example/MyClass.class", t3);

        // 4. Missing both
        String t4 = JavadecMcpServer.resolveTarget(Map.of());
        assertNull(t4);
    }

    @Test
    public void testValidateTarget() {
        // Valid archive target
        assertDoesNotThrow(() -> {
            JavadecMcpServer.validateTarget(refJar.getAbsolutePath() + "!com/joutvhu/javadec/test/SampleService.class");
        });

        // Null target throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            JavadecMcpServer.validateTarget(null);
        });

        // Non-existent archive throws FileNotFoundException
        assertThrows(FileNotFoundException.class, () -> {
            JavadecMcpServer.validateTarget("nonexistent.jar!com/example/MyClass.class");
        });

        // Target with folder containing exclamation mark
        FileNotFoundException ex = assertThrows(FileNotFoundException.class, () -> {
            JavadecMcpServer.validateTarget("my!folder/archive.jar!com/example/MyClass.class");
        });
        assertTrue(ex.getMessage().contains("my!folder/archive.jar"));

        // Passing archive directly without class throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            JavadecMcpServer.validateTarget(refJar.getAbsolutePath());
        });
    }

    @Test
    public void testFindReferencesExecution() {
        com.joutvhu.javadec.core.api.JavaDecompiler decompiler = new com.joutvhu.javadec.core.api.JavaDecompiler();
        assertDoesNotThrow(() -> {
            var res = decompiler.findReferences(refJar, "#main", 10, null);
            assertNotNull(res);
            assertNotNull(res.formattedSummary());
        });
    }

    @Test
    public void testDisassembleBytecodeExecution() {
        com.joutvhu.javadec.core.api.JavaDecompiler decompiler = new com.joutvhu.javadec.core.api.JavaDecompiler();
        assertDoesNotThrow(() -> {
            String res = decompiler.disassembleArchiveClass(refJar, "com/joutvhu/javadec/test/SampleService.class", "main", null, null);
            assertNotNull(res);
            assertTrue(res.contains("GETSTATIC") || res.contains("INVOKEVIRTUAL") || res.contains("RETURN"));
        });
    }

    @Test
    public void testExtractStringsExecution() {
        com.joutvhu.javadec.core.api.JavaDecompiler decompiler = new com.joutvhu.javadec.core.api.JavaDecompiler();
        assertDoesNotThrow(() -> {
            var res = decompiler.extractStrings(refJar, null, null, 3, 10);
            assertNotNull(res);
            assertNotNull(res.formattedSummary());
        });
    }

    @Test
    public void testInspectStackTraceLineExecution() {
        com.joutvhu.javadec.core.api.JavaDecompiler decompiler = new com.joutvhu.javadec.core.api.JavaDecompiler();
        assertDoesNotThrow(() -> {
            var res = decompiler.inspectStackTraceLine(refJar, "com/joutvhu/javadec/test/SampleService.class", 4, 2);
            assertNotNull(res);
            assertNotNull(res.snippet());
        });
    }

    @Test
    public void testJarDiffExecution() {
        com.joutvhu.javadec.core.api.JavaDecompiler decompiler = new com.joutvhu.javadec.core.api.JavaDecompiler();
        assertDoesNotThrow(() -> {
            var res = decompiler.compareJarApis(refJar, refJar, null, false);
            assertNotNull(res);
            assertNotNull(res.formattedReport());
        });
    }

    @Test
    public void testGetCalleesExecution() {
        com.joutvhu.javadec.core.api.JavaDecompiler decompiler = new com.joutvhu.javadec.core.api.JavaDecompiler();
        assertDoesNotThrow(() -> {
            var res = decompiler.getArchiveCallees(refJar, "com/joutvhu/javadec/test/SampleService.class", "main", 10);
            assertNotNull(res);
            assertNotNull(res.formattedSummary());
        });
    }

    @Test
    public void testAnalyzeDependenciesExecution() {
        com.joutvhu.javadec.core.api.JavaDecompiler decompiler = new com.joutvhu.javadec.core.api.JavaDecompiler();
        assertDoesNotThrow(() -> {
            var res = decompiler.analyzeArchiveDependencies(refJar, "com/joutvhu/javadec/test/SampleService.class");
            assertNotNull(res);
            assertNotNull(res.formattedReport());
        });
    }

    @Test
    public void testInspectManifestExecution() {
        com.joutvhu.javadec.core.api.JavaDecompiler decompiler = new com.joutvhu.javadec.core.api.JavaDecompiler();
        assertDoesNotThrow(() -> {
            var res = decompiler.inspectManifest(refJar);
            assertNotNull(res);
            assertNotNull(res.formattedSummary());
        });
    }
}
