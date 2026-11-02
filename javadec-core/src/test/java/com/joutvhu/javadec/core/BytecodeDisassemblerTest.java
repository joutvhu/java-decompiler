package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.disasm.BytecodeDisassembler;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class BytecodeDisassemblerTest {

    @Test
    public void testDisassembleFullClass() {
        String src = """
                package com.example;
                public class Calc {
                    public int add(int a, int b) {
                        return a + b;
                    }
                    public int sub(int a, int b) {
                        return a - b;
                    }
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.Calc", src);
        byte[] bytes = classes.get("com.example.Calc");
        assertNotNull(bytes);

        String disasm = BytecodeDisassembler.disassemble(bytes, null, null, null);
        assertNotNull(disasm);
        assertTrue(disasm.contains("ILOAD 1"));
        assertTrue(disasm.contains("ILOAD 2"));
        assertTrue(disasm.contains("IADD"));
        assertTrue(disasm.contains("ISUB"));
        assertTrue(disasm.contains("IRETURN"));
    }

    @Test
    public void testDisassembleSingleMethod() {
        String src = """
                package com.example;
                public class Calc {
                    public int add(int a, int b) {
                        return a + b;
                    }
                    public int sub(int a, int b) {
                        return a - b;
                    }
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.Calc", src);
        byte[] bytes = classes.get("com.example.Calc");
        assertNotNull(bytes);

        String disasm = BytecodeDisassembler.disassemble(bytes, "add", null, null);
        assertNotNull(disasm);
        assertTrue(disasm.contains("IADD"));
        assertFalse(disasm.contains("ISUB")); // sub method MUST NOT be present
    }

    @Test
    public void testDisassembleWithPagination() {
        String src = """
                package com.example;
                public class Demo {
                    public void loop() {
                        for (int i = 0; i < 100; i++) {
                            System.out.println(i);
                        }
                    }
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.Demo", src);
        byte[] bytes = classes.get("com.example.Demo");

        String paged = BytecodeDisassembler.disassemble(bytes, null, 1, 5);
        assertNotNull(paged);
        String[] lines = paged.split("\n");
        assertTrue(lines.length <= 5);
    }
}
