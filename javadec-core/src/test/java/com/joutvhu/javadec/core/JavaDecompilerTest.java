package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.api.DecompileOptions;
import com.joutvhu.javadec.core.api.JavaDecompiler;
import com.joutvhu.javadec.core.model.ArchiveTree;
import com.joutvhu.javadec.core.model.ClassOutline;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JavaDecompilerTest {

    @Test
    public void testFullDecompilerCapabilities(@TempDir Path tempDir) throws Exception {
        JavaDecompiler decompiler = new JavaDecompiler();
        File jarFile = new File("../.source/jetbrains/java-decompiler.jar");
        assertTrue(jarFile.exists(), "Reference jar must exist");

        // 1. Test inspectArchiveTree
        ArchiveTree tree = decompiler.inspectArchiveTree(jarFile);
        assertNotNull(tree);
        assertTrue(tree.totalClasses() > 0, "Classes count must be > 0");
        assertTrue(tree.totalResources() > 0, "Resources count must be > 0");
        System.out.println("=== ARCHIVE TREE ===");
        System.out.println(tree.formattedTree());

        // 2. Test readArchiveResource (plugin.xml)
        String pluginXml = decompiler.readArchiveResource(jarFile, "META-INF/plugin.xml");
        assertNotNull(pluginXml);
        assertTrue(pluginXml.contains("<idea-plugin"), "Resource should contain <idea-plugin");
        System.out.println("=== RESOURCE PREVIEW ===");
        System.out.println(pluginXml.substring(0, Math.min(200, pluginXml.length())));

        // 3. Test listArchiveClasses
        List<String> classes = decompiler.listArchiveClasses(jarFile, "ConsoleDecompiler");
        assertFalse(classes.isEmpty(), "ConsoleDecompiler class must be found");
        String targetClass = classes.get(0);

        // 4. Test getArchiveClassOutline
        ClassOutline outline = decompiler.getArchiveClassOutline(jarFile, targetClass);
        assertNotNull(outline);
        assertEquals("ConsoleDecompiler", outline.className().substring(outline.className().lastIndexOf('/') + 1));
        String skeleton = outline.toSkeletonSource();
        System.out.println("=== SKELETON SOURCE ===");
        System.out.println(skeleton);
        assertTrue(skeleton.contains("ConsoleDecompiler"));

        // 5. Test decompileArchiveClass
        String source = decompiler.decompileArchiveClass(jarFile, targetClass, DecompileOptions.defaults());
        assertNotNull(source);
        assertTrue(source.contains("ConsoleDecompiler"));

        // 6. Test decompileArchiveMethod
        String methodSource = decompiler.decompileArchiveMethod(jarFile, targetClass, "main", DecompileOptions.defaults());
        assertNotNull(methodSource);
        System.out.println("=== METHOD SOURCE ===");
        System.out.println(methodSource);
        assertTrue(methodSource.contains("main"));

        // 7. Test extractArchive
        File outputDir = tempDir.resolve("extracted").toFile();
        int extracted = decompiler.extractArchive(jarFile, outputDir, "META-INF/plugin.xml");
        assertTrue(extracted > 0, "Must extract plugin.xml");
        assertTrue(new File(outputDir, "META-INF/plugin.xml").exists(), "Extracted file must exist on disk");
    }
}
