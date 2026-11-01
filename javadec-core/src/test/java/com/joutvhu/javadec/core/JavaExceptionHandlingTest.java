package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.api.DecompileOptions;
import com.joutvhu.javadec.core.api.JavaDecompiler;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JavaExceptionHandlingTest {

    private final JavaDecompiler decompiler = new JavaDecompiler();

    @Test
    public void testTryCatchFinallyAndMultiCatch() {
        String source = """
                package com.example.exceptions;
                import java.io.IOException;
                public class ExceptionDemo {
                    public String readFile(boolean fail) {
                        try {
                            if (fail) {
                                throw new IOException("IO error");
                            }
                            return "success";
                        } catch (IOException | RuntimeException e) {
                            return "error: " + e.getMessage();
                        } finally {
                            System.out.println("finally block");
                        }
                    }
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.exceptions.ExceptionDemo", source);
        String decompiled = decompiler.decompile("com/example/exceptions/ExceptionDemo", classes.get("com.example.exceptions.ExceptionDemo"), DecompileOptions.defaults());
        assertNotNull(decompiled);
        assertTrue(decompiled.contains("try {"));
        assertTrue(decompiled.contains("catch ("));
        assertTrue(decompiled.contains("finally {"));
        assertTrue(decompiled.contains("finally block"));
    }

    @Test
    public void testTryWithResources() {
        String source = """
                package com.example.exceptions;
                import java.io.ByteArrayInputStream;
                import java.io.IOException;
                public class TryWithResourcesDemo {
                    public int readByte(byte[] data) throws IOException {
                        try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
                            return in.read();
                        }
                    }
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.exceptions.TryWithResourcesDemo", source);
        String decompiled = decompiler.decompile("com/example/exceptions/TryWithResourcesDemo", classes.get("com.example.exceptions.TryWithResourcesDemo"), DecompileOptions.defaults());
        assertNotNull(decompiled);
        assertTrue(decompiled.contains("try (ByteArrayInputStream in = new ByteArrayInputStream(data)"));
        assertTrue(decompiled.contains("in.read()"));
    }
}
