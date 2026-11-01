package com.joutvhu.javadec.mcp;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JavadecMcpServerTest {

    private final File refJar = new File("../.source/jetbrains/java-decompiler.jar");

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
            JavadecMcpServer.validateTarget(refJar.getAbsolutePath() + "!org/jetbrains/java/decompiler/main/decompiler/ConsoleDecompiler.class");
        });

        // Null target throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            JavadecMcpServer.validateTarget(null);
        });

        // Non-existent archive throws FileNotFoundException
        assertThrows(FileNotFoundException.class, () -> {
            JavadecMcpServer.validateTarget("nonexistent.jar!com/example/MyClass.class");
        });

        // Passing archive directly without class throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            JavadecMcpServer.validateTarget(refJar.getAbsolutePath());
        });
    }
}
