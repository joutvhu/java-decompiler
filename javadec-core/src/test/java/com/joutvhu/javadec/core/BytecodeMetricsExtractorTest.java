package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.model.ClassMetricsResult;
import com.joutvhu.javadec.core.scanner.BytecodeMetricsExtractor;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class BytecodeMetricsExtractorTest {

    @Test
    public void testExtractBytecodeMetrics() {
        String src = """
                package com.example;
                public class MetricsDemo {
                    private int count;
                    public int calculate(int a, int b) {
                        try {
                            return a / b;
                        } catch (ArithmeticException e) {
                            return 0;
                        }
                    }
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.MetricsDemo", src);
        byte[] bytes = classes.get("com.example.MetricsDemo");
        assertNotNull(bytes);

        ClassMetricsResult result = BytecodeMetricsExtractor.extract(bytes, "calculate");
        assertNotNull(result);
        assertEquals(1, result.methods().size());

        var m = result.methods().get(0);
        assertEquals("calculate", m.methodName());
        assertTrue(m.instructionCount() > 0);
        assertTrue(m.tryCatchCount() >= 1, "Should count try-catch block");
        assertTrue(m.maxStack() > 0);

        String summary = result.formattedSummary();
        assertTrue(summary.contains("calculate"));
        assertTrue(summary.contains("try-catch"));
    }
}
