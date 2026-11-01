package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.api.DecompileOptions;
import com.joutvhu.javadec.core.api.JavaDecompiler;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JavaModernFeaturesTest {

    private final JavaDecompiler decompiler = new JavaDecompiler();

    @Test
    public void testJavaRecordDecompilation() {
        String source = """
                package com.example.modern;
                public record Point(int x, int y) {
                    public int distanceSquared() {
                        return x * x + y * y;
                    }
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.modern.Point", source);
        String decompiled = decompiler.decompile("com/example/modern/Point", classes.get("com.example.modern.Point"), DecompileOptions.defaults());
        assertNotNull(decompiled);
        assertTrue(decompiled.contains("record Point(int x, int y)") || decompiled.contains("Point(int x, int y)"));
        assertTrue(decompiled.contains("distanceSquared()"));
    }

    @Test
    public void testPatternMatchingForInstanceOf() {
        String source = """
                package com.example.modern;
                public class PatternMatchDemo {
                    public int describe(Object obj) {
                        if (obj instanceof String s) {
                            return s.length();
                        } else if (obj instanceof Integer i) {
                            return i * 2;
                        }
                        return -1;
                    }
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.modern.PatternMatchDemo", source);
        String decompiled = decompiler.decompile("com/example/modern/PatternMatchDemo", classes.get("com.example.modern.PatternMatchDemo"), DecompileOptions.defaults());
        assertNotNull(decompiled);
        assertTrue(decompiled.contains("instanceof String") || decompiled.contains("instanceof"));
        assertTrue(decompiled.contains("length()"));
    }

    @Test
    public void testSealedClassAndInterfaces() {
        String source = """
                package com.example.modern;
                public class SealedDemo {
                    public sealed interface Shape permits Circle, Square {}
                    public static final class Circle implements Shape {}
                    public static final class Square implements Shape {}
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.modern.SealedDemo", source);
        // Pass classes map to resolve inner sealed classes
        String decompiled = decompiler.decompile("com/example/modern/SealedDemo", classes.get("com.example.modern.SealedDemo"), classes, DecompileOptions.defaults());
        assertNotNull(decompiled);
        assertTrue(decompiled.contains("Shape"));
        assertTrue(decompiled.contains("Circle"));
        assertTrue(decompiled.contains("Square"));
    }
}
