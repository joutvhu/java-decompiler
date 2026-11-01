package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.api.DecompileOptions;
import com.joutvhu.javadec.core.api.JavaDecompiler;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JavaLoopDecompilationTest {

    private final JavaDecompiler decompiler = new JavaDecompiler();

    @Test
    public void testStandardForLoop() {
        String source = """
                package com.example.loops;
                public class ForLoopDemo {
                    public int sum(int n) {
                        int total = 0;
                        for (int i = 0; i < n; i++) {
                            total += i;
                        }
                        return total;
                    }
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.loops.ForLoopDemo", source);
        String decompiled = decompiler.decompile("com/example/loops/ForLoopDemo", classes.get("com.example.loops.ForLoopDemo"), DecompileOptions.defaults());
        assertNotNull(decompiled);
        assertTrue(decompiled.contains("for ("));
        assertTrue(decompiled.contains("total += i") || decompiled.contains("total = total + i"));
    }

    @Test
    public void testEnhancedForEachLoop() {
        String source = """
                package com.example.loops;
                import java.util.List;
                public class ForEachDemo {
                    public int countLengths(List<String> items) {
                        int count = 0;
                        for (String item : items) {
                            count += item.length();
                        }
                        return count;
                    }
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.loops.ForEachDemo", source);
        String decompiled = decompiler.decompile("com/example/loops/ForEachDemo", classes.get("com.example.loops.ForEachDemo"), DecompileOptions.defaults());
        assertNotNull(decompiled);
        assertTrue(decompiled.contains("for (String item : items)"));
    }

    @Test
    public void testWhileAndDoWhileLoops() {
        String source = """
                package com.example.loops;
                public class WhileDemo {
                    public int runWhile(int count) {
                        while (count > 0) {
                            count--;
                        }
                        return count;
                    }
                    public int runDoWhile(int count) {
                        do {
                            count++;
                        } while (count < 10);
                        return count;
                    }
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.loops.WhileDemo", source);
        String decompiled = decompiler.decompile("com/example/loops/WhileDemo", classes.get("com.example.loops.WhileDemo"), DecompileOptions.defaults());
        assertNotNull(decompiled);
        assertTrue(decompiled.contains("while (count > 0)") || decompiled.contains("while ("));
        assertTrue(decompiled.contains("while ("));
    }

    @Test
    public void testNestedLoopsWithLabeledBreakAndContinue() {
        String source = """
                package com.example.loops;
                public class LabeledLoopDemo {
                    public int findItem(int[][] matrix, int target) {
                        int found = -1;
                        outer:
                        for (int i = 0; i < matrix.length; i++) {
                            for (int j = 0; j < matrix[i].length; j++) {
                                if (matrix[i][j] == target) {
                                    found = target;
                                    break outer;
                                }
                                if (matrix[i][j] < 0) {
                                    continue outer;
                                }
                            }
                        }
                        return found;
                    }
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.loops.LabeledLoopDemo", source);
        String decompiled = decompiler.decompile("com/example/loops/LabeledLoopDemo", classes.get("com.example.loops.LabeledLoopDemo"), DecompileOptions.defaults());
        assertNotNull(decompiled);
        assertTrue(decompiled.contains("for ("));
        assertTrue(decompiled.contains("break"));
    }
}
