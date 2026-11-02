package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.api.JavaDecompiler;
import com.joutvhu.javadec.core.model.StackTraceSnippetResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class StackTraceContextExtractorTest {

    @Test
    public void testInspectStackTraceSnippet(@TempDir Path tempDir) throws Exception {
        String src = """
                package com.example;
                public class PaymentService {
                    public boolean charge(int amount) {
                        if (amount <= 0) {
                            throw new IllegalArgumentException("Amount must be positive");
                        }
                        return true;
                    }
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.PaymentService", src);
        File jar = tempDir.resolve("trace-test.jar").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(jar))) {
            for (var entry : classes.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey().replace('.', '/') + ".class"));
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }

        JavaDecompiler decompiler = new JavaDecompiler();
        StackTraceSnippetResult res = decompiler.inspectStackTraceLine(jar, "com/example/PaymentService.class", 5, 2);
        assertNotNull(res);
        assertNotNull(res.snippet());
        assertTrue(res.snippet().contains("PaymentService"));
        assertEquals("charge", res.methodName());
        assertTrue(res.snippet().contains(">>>"), "Snippet should contain highlighted marker >>>");
    }
}
