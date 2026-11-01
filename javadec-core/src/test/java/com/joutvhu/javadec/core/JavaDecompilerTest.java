package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.api.DecompileOptions;
import com.joutvhu.javadec.core.api.JavaDecompiler;
import com.joutvhu.javadec.core.loader.PathUtil;
import com.joutvhu.javadec.core.loader.ZipBytecodeLoader;
import com.joutvhu.javadec.core.model.ArchiveTree;
import com.joutvhu.javadec.core.model.ClassOutline;
import com.joutvhu.javadec.core.model.ExportResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

        // 2. Test readArchiveResource with pagination (lines 1 to 5)
        String pluginXml = decompiler.readArchiveResource(jarFile, "META-INF/plugin.xml", 1, 5);
        assertNotNull(pluginXml);
        assertEquals(5, pluginXml.split("\r?\n").length, "Should read exactly 5 lines");

        // 3. Test listArchiveClasses with glob / package pattern
        List<String> classes = decompiler.listArchiveClasses(jarFile, "*ConsoleDecompiler*");
        assertFalse(classes.isEmpty(), "ConsoleDecompiler class must be found");
        String targetClass = classes.get(0);

        // 4. Test getArchiveClassOutline
        ClassOutline outline = decompiler.getArchiveClassOutline(jarFile, targetClass);
        assertNotNull(outline);
        assertEquals("ConsoleDecompiler", outline.className().substring(outline.className().lastIndexOf('/') + 1));
        String skeleton = outline.toSkeletonSource();
        assertTrue(skeleton.contains("ConsoleDecompiler"));

        // 5. Test inner class resolution with dot notation (e.g. ClassWriter.MType instead of $)
        byte[] innerBytes = ZipBytecodeLoader.loadClassBytes(jarFile, "org/jetbrains/java/decompiler/main/ClassWriter.MType.class");
        assertNotNull(innerBytes, "Must resolve inner class with dot notation");
        ClassOutline innerOutline = decompiler.getOutline(innerBytes);
        assertNotNull(innerOutline);
        assertTrue(innerOutline.className().contains("ClassWriter$MType"));

        // 6. Test decompileArchiveClass with line pagination (lines 10 to 15)
        DecompileOptions pagedOptions = DecompileOptions.builder().lineOffset(10).lineLimit(6).build();
        String source = decompiler.decompileArchiveClass(jarFile, targetClass, pagedOptions);
        assertNotNull(source);
        assertEquals(6, source.split("\r?\n").length, "Decompiled source should have 6 paginated lines");

        // 7. Test decompileArchiveMethod
        String methodSource = decompiler.decompileArchiveMethod(jarFile, targetClass, "main", DecompileOptions.defaults());
        assertNotNull(methodSource);
        assertTrue(methodSource.contains("main"));

        // 8. Test extractArchive (raw extraction)
        File rawExtractDir = tempDir.resolve("raw").toFile();
        int extracted = decompiler.extractArchive(jarFile, rawExtractDir, "ConsoleDecompiler");
        assertTrue(extracted > 0, "Must extract ConsoleDecompiler");
        File extractedClassFile = new File(rawExtractDir, "org/jetbrains/java/decompiler/main/decompiler/ConsoleDecompiler.class");
        assertTrue(extractedClassFile.exists(), "Extracted .class file must exist on disk");

        // 9. Test standalone file methods (getClassOutline, decompileFile, decompileFileMethod)
        ClassOutline standaloneOutline = decompiler.getClassOutline(extractedClassFile);
        assertNotNull(standaloneOutline);
        assertEquals("ConsoleDecompiler", standaloneOutline.className().substring(standaloneOutline.className().lastIndexOf('/') + 1));

        String standaloneSource = decompiler.decompileFile(extractedClassFile, DecompileOptions.defaults());
        assertNotNull(standaloneSource);
        assertTrue(standaloneSource.contains("ConsoleDecompiler"));

        String standaloneMethod = decompiler.decompileFileMethod(extractedClassFile, "main", DecompileOptions.defaults());
        assertNotNull(standaloneMethod);
        assertTrue(standaloneMethod.contains("main"));

        // 10. Test exportArchive (full decompilation to .java files)
        File decompiledDir = tempDir.resolve("decompiled").toFile();
        ExportResult exportResult = decompiler.exportArchive(jarFile, decompiledDir, "ConsoleDecompiler", DecompileOptions.defaults());
        assertNotNull(exportResult);
        assertTrue(exportResult.decompiledClasses() > 0, "Must decompile at least 1 class");
        File expectedJavaFile = new File(decompiledDir, "org/jetbrains/java/decompiler/main/decompiler/ConsoleDecompiler.java");
        assertTrue(expectedJavaFile.exists(), "Decompiled .java file must exist on disk at " + expectedJavaFile.getAbsolutePath());

        // 11. Test Companion Sources JAR resolution (preserves Javadoc and comments)
        File mockBinJar = tempDir.resolve("demo-lib-1.0.jar").toFile();
        File mockSrcJar = tempDir.resolve("demo-lib-1.0-sources.jar").toFile();

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(mockBinJar))) {
            zos.putNextEntry(new ZipEntry("com/example/Demo.class"));
            byte[] dummyBytes = ZipBytecodeLoader.loadClassBytes(jarFile, targetClass);
            zos.write(dummyBytes);
            zos.closeEntry();
        }

        String sampleSourceWithJavadoc = """
                package com.example;
                
                /**
                 * Service handling core order management operations.
                 *
                 * @author Developer
                 * @version 1.0
                 */
                public class Demo {
                    /**
                     * Processes the given order.
                     *
                     * @param orderId the order identifier
                     * @return true if successful
                     */
                    public boolean process(String orderId) {
                        // Business verification logic
                        return orderId != null;
                    }
                }
                """;
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(mockSrcJar))) {
            zos.putNextEntry(new ZipEntry("com/example/Demo.java"));
            zos.write(sampleSourceWithJavadoc.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        String resolvedSource = decompiler.decompileArchiveClass(mockBinJar, "com/example/Demo.class", DecompileOptions.defaults());
        assertNotNull(resolvedSource);
        assertTrue(resolvedSource.contains("Service handling core order management operations"), "Must contain class Javadoc");
        assertTrue(resolvedSource.contains("Processes the given order"), "Must contain method Javadoc");
        assertTrue(resolvedSource.contains("// Business verification logic"), "Must contain inline comments");

        // 12. Test PathUtil ~ and $HOME expansion
        String userHome = System.getProperty("user.home").replace('\\', '/');
        assertEquals(userHome + "/.m2/repository", PathUtil.expandUserHome("~/.m2/repository"));
        assertEquals(userHome + "/.m2/repository", PathUtil.expandUserHome("$HOME/.m2/repository"));
    }
}
