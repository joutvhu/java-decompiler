package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.api.DecompileOptions;
import com.joutvhu.javadec.core.api.JavaDecompiler;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JavaControlFlowTest {

    private final JavaDecompiler decompiler = new JavaDecompiler();

    @Test
    public void testNestedIfElseAndTernary() {
        String source = """
                package com.example.flow;
                public class IfElseDemo {
                    public String classify(int score) {
                        if (score >= 90) {
                            return "A";
                        } else if (score >= 80) {
                            return "B";
                        } else if (score >= 70) {
                            return "C";
                        } else {
                            return score >= 60 ? "D" : "F";
                        }
                    }
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.flow.IfElseDemo", source);
        String decompiled = decompiler.decompile("com/example/flow/IfElseDemo", classes.get("com.example.flow.IfElseDemo"), DecompileOptions.defaults());
        assertNotNull(decompiled);
        assertTrue(decompiled.contains("if (score >= 90)"));
        assertTrue(decompiled.contains("\"A\""));
        assertTrue(decompiled.contains("\"B\""));
        assertTrue(decompiled.contains("\"F\""));
    }

    @Test
    public void testSwitchOnStringAndEnum() {
        String source = """
                package com.example.flow;
                public class SwitchDemo {
                    public enum Mode { FAST, SLOW, DEFAULT }

                    public int parseMode(Mode mode) {
                        switch (mode) {
                            case FAST: return 1;
                            case SLOW: return 2;
                            default: return 0;
                        }
                    }

                    public int parseString(String command) {
                        switch (command) {
                            case "start": return 1;
                            case "stop": return 2;
                            case "pause": return 3;
                            default: return -1;
                        }
                    }
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.flow.SwitchDemo", source);
        // Pass classes map so SwitchDemo$Mode inner class is resolved
        String decompiled = decompiler.decompile("com/example/flow/SwitchDemo", classes.get("com.example.flow.SwitchDemo"), classes, DecompileOptions.defaults());
        assertNotNull(decompiled);
        assertTrue(decompiled.contains("switch (mode)") || decompiled.contains("switch"));
        assertTrue(decompiled.contains("FAST") && decompiled.contains("SLOW"));
        assertTrue(decompiled.contains("\"start\""));
        assertTrue(decompiled.contains("\"stop\""));
    }

    @Test
    public void testModernSwitchExpression() {
        String source = """
                package com.example.flow;
                public class SwitchExpressionDemo {
                    public int evaluate(int op, int a, int b) {
                        return switch (op) {
                            case 1 -> a + b;
                            case 2 -> a - b;
                            case 3 -> a * b;
                            default -> 0;
                        };
                    }
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.flow.SwitchExpressionDemo", source);
        String decompiled = decompiler.decompile("com/example/flow/SwitchExpressionDemo", classes.get("com.example.flow.SwitchExpressionDemo"), DecompileOptions.defaults());
        assertNotNull(decompiled);
        assertTrue(decompiled.contains("switch (op)"));
        assertTrue(decompiled.contains("case 1"));
        assertTrue(decompiled.contains("a + b") || decompiled.contains("return"));
    }
}
