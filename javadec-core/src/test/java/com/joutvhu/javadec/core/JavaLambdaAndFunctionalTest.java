package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.api.DecompileOptions;
import com.joutvhu.javadec.core.api.JavaDecompiler;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JavaLambdaAndFunctionalTest {

    private final JavaDecompiler decompiler = new JavaDecompiler();

    @Test
    public void testLambdaExpressionsAndStreams() {
        String source = """
                package com.example.functional;
                import java.util.List;
                import java.util.stream.Collectors;
                public class LambdaDemo {
                    public List<String> filterAndTransform(List<String> items, String prefix) {
                        return items.stream()
                                .filter(s -> s.startsWith(prefix))
                                .map(String::toUpperCase)
                                .collect(Collectors.toList());
                    }
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.functional.LambdaDemo", source);
        String decompiled = decompiler.decompile("com/example/functional/LambdaDemo", classes.get("com.example.functional.LambdaDemo"), DecompileOptions.defaults());
        assertNotNull(decompiled);
        assertTrue(decompiled.contains("->") || decompiled.contains("filter"));
        assertTrue(decompiled.contains("toUpperCase") || decompiled.contains("String::toUpperCase"));
        assertTrue(decompiled.contains("stream()"));
    }

    @Test
    public void testMethodReferencesAndCapturedVariables() {
        String source = """
                package com.example.functional;
                import java.util.function.Function;
                public class MethodRefDemo {
                    public Function<String, Integer> makeParser(int multiplier) {
                        return s -> Integer.parseInt(s) * multiplier;
                    }
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.functional.MethodRefDemo", source);
        String decompiled = decompiler.decompile("com/example/functional/MethodRefDemo", classes.get("com.example.functional.MethodRefDemo"), DecompileOptions.defaults());
        assertNotNull(decompiled);
        assertTrue(decompiled.contains("->"));
        assertTrue(decompiled.contains("multiplier"));
    }
}
