package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.diff.JarApiDiffEngine;
import com.joutvhu.javadec.core.model.JarApiDiffResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class JarApiDiffEngineTest {

    @Test
    public void testJarDiffDetectsAddedRemovedModified(@TempDir Path tempDir) throws Exception {
        String v1Src = """
                package com.example;
                public class Service {
                    public int status = 1;
                    public void oldMethod() {}
                    public int calculate(int x) { return x; }
                }
                """;

        String v2Src = """
                package com.example;
                public class Service {
                    public String status = "active";
                    public int calculate(int x) { return x * 2; }
                    public void newMethod() {}
                }
                """;

        Map<String, byte[]> c1 = TestCompiler.compile("com.example.Service", v1Src);
        Map<String, byte[]> c2 = TestCompiler.compile(Map.of(
                "com.example.Service", v2Src,
                "com.example.NewClass", "package com.example; public class NewClass { public void hello() {} }"
        ));

        File jar1 = tempDir.resolve("v1.jar").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(jar1))) {
            for (var entry : c1.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey().replace('.', '/') + ".class"));
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }

        File jar2 = tempDir.resolve("v2.jar").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(jar2))) {
            for (var entry : c2.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey().replace('.', '/') + ".class"));
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }

        JarApiDiffResult diff = JarApiDiffEngine.compare(jar1, jar2, null, false);
        assertNotNull(diff);
        assertTrue(diff.addedCount() >= 2); // newMethod + NewClass
        assertTrue(diff.removedCount() >= 1); // oldMethod
        assertTrue(diff.modifiedCount() >= 1); // status field modified

        String report = diff.formattedReport();
        assertTrue(report.contains("[REMOVED] com.example.Service.oldMethod"));
        assertTrue(report.contains("[ADDED]   com.example.Service.newMethod"));
        assertTrue(report.contains("[ADDED]   com.example.NewClass"));
        assertTrue(report.contains("[CHANGED] com.example.Service.status"));
    }
}
