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

        // 1. Test inspectArchiveTree with path and depth
        ArchiveTree tree = decompiler.inspectArchiveTree(jarFile, "META-INF", 2);
        assertNotNull(tree);
        assertTrue(tree.formattedTree().contains("plugin.xml"), "Tree filtered to META-INF should contain plugin.xml");
        System.out.println("=== ARCHIVE TREE (META-INF, depth 2) ===");
        System.out.println(tree.formattedTree());

        // 2. Test readArchiveResource with pagination (lines 1 to 5)
        String pluginXml = decompiler.readArchiveResource(jarFile, "META-INF/plugin.xml", 1, 5);
        assertNotNull(pluginXml);
        assertEquals(5, pluginXml.split("\r?\n").length, "Should read exactly 5 lines");
        System.out.println("=== RESOURCE PAGINATED (5 lines) ===");
        System.out.println(pluginXml);

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

        // 5. Test decompileArchiveClass with line pagination (lines 10 to 15)
        DecompileOptions pagedOptions = DecompileOptions.builder().lineOffset(10).lineLimit(6).build();
        String source = decompiler.decompileArchiveClass(jarFile, targetClass, pagedOptions);
        assertNotNull(source);
        assertEquals(6, source.split("\r?\n").length, "Decompiled source should have 6 paginated lines");

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
