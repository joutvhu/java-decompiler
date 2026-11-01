package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.api.JavaDecompiler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MethodExtractorTest {

    private final JavaDecompiler decompiler = new JavaDecompiler();

    @Test
    public void testExtractMethodWithDoubleBackslashInString() {
        String code = """
                package com.example;
                public class Demo {
                    public void printPath() {
                        System.out.println("Path: c:\\\\my\\\\source\\\\");
                        int x = 42;
                    }
                    public void nextMethod() {
                        System.out.println("Next");
                    }
                }
                """;

        String extracted = decompiler.extractMethodFromSource(code, "printPath");
        assertNotNull(extracted);
        assertTrue(extracted.contains("public void printPath() {"));
        assertTrue(extracted.contains("int x = 42;"));
        assertTrue(extracted.endsWith("}"));
        assertFalse(extracted.contains("nextMethod")); // MUST NOT leak nextMethod
    }

    @Test
    public void testExtractMethodWithNestedBracesAndComments() {
        String code = """
                package com.example;
                public class Demo {
                    /**
                     * Process task with runnable.
                     */
                    @Override
                    public void execute() {
                        // comment with closing brace }
                        /* block comment with { and } */
                        String json = "{\\"key\\": \\"value\\"}";
                        char brace = '}';
                        Runnable r = () -> {
                            System.out.println("Hello");
                        };
                        r.run();
                    }
                    public void following() {}
                }
                """;

        String extracted = decompiler.extractMethodFromSource(code, "execute");
        assertNotNull(extracted);
        assertTrue(extracted.contains("Process task with runnable")); // Javadoc preserved
        assertTrue(extracted.contains("@Override")); // Annotation preserved
        assertTrue(extracted.contains("public void execute()"));
        assertTrue(extracted.contains("String json = \"{\\\"key\\\": \\\"value\\\"}\";"));
        assertTrue(extracted.contains("r.run();"));
        assertTrue(extracted.endsWith("}"));
        assertFalse(extracted.contains("following"));
    }

    @Test
    public void testExtractOverloadsWithParameterHint() {
        String code = """
                package com.example;
                public class Service {
                    public void save() {
                        System.out.println("Zero args");
                    }
                    public void save(String id, int timeout) {
                        System.out.println("Two args: " + id);
                    }
                    public void save(String id, int timeout, boolean force) {
                        System.out.println("Three args");
                    }
                }
                """;

        // 1. Without hint -> returns all 3 overloads
        String allOverloads = decompiler.extractMethodFromSource(code, "save");
        assertTrue(allOverloads.contains("Zero args"));
        assertTrue(allOverloads.contains("Two args"));
        assertTrue(allOverloads.contains("Three args"));

        // 2. With hint -> returns matching overload
        String specific = decompiler.extractMethodFromSource(code, "save(String, int)");
        assertTrue(specific.contains("Two args"));
        assertFalse(specific.contains("Three args"));
    }

    @Test
    public void testExtractOverloadsWithGenericTypesAndAnnotationsHavingCommas() {
        String code = """
                package com.example;
                import java.util.Map;
                public class Service {
                    public void query(@RequestParam(name = "q", required = false) Map<String, Object> params, int limit) {
                        System.out.println("Complex overload");
                    }
                    public void query(String simple) {
                        System.out.println("Simple overload");
                    }
                }
                """;

        String result = decompiler.extractMethodFromSource(code, "query(Map, int)");
        assertTrue(result.contains("Complex overload"));
        assertFalse(result.contains("Simple overload"));
    }

    @Test
    public void testExtractOverloadDistinguishesSubstringTypes() {
        String code = """
                package com.example;
                import java.io.PrintStream;
                public class Printer {
                    public void print(PrintStream ps) {
                        ps.println();
                    }
                    public void print(int x) {
                        System.out.println(x);
                    }
                    public void print(int[] arr) {
                        System.out.println(arr.length);
                    }
                }
                """;

        // "int" must not falsely match "PrintStream" or "int[]"
        String intResult = decompiler.extractMethodFromSource(code, "print(int)");
        assertTrue(intResult.contains("public void print(int x)"));
        assertFalse(intResult.contains("PrintStream"));
        assertFalse(intResult.contains("int[]"));

        // "int[]" must match array overload
        String arrResult = decompiler.extractMethodFromSource(code, "print(int[])");
        assertTrue(arrResult.contains("public void print(int[] arr)"));
        assertFalse(arrResult.contains("PrintStream"));
        assertFalse(arrResult.contains("int x"));
    }

    @Test
    public void testExtractInterfaceMethod() {
        String code = """
                package com.example;
                public interface Repository {
                    /**
                     * Finds entity by ID.
                     */
                    User findById(String id);
                    void delete(String id);
                }
                """;

        String extracted = decompiler.extractMethodFromSource(code, "findById");
        assertNotNull(extracted);
        assertTrue(extracted.contains("Finds entity by ID"));
        assertTrue(extracted.contains("User findById(String id);"));
        assertFalse(extracted.contains("delete"));
    }

    @Test
    public void testExtractStaticInitializer() {
        String code = """
                package com.example;
                public class NativeLib {
                    static {
                        System.loadLibrary("native");
                        System.out.println("Loaded");
                    }
                    public void call() {}
                }
                """;

        String extracted = decompiler.extractMethodFromSource(code, "static");
        assertNotNull(extracted);
        assertTrue(extracted.contains("System.loadLibrary(\"native\");"));
        assertFalse(extracted.contains("call"));

        String extractedClinit = decompiler.extractMethodFromSource(code, "<clinit>");
        assertNotNull(extractedClinit);
        assertTrue(extractedClinit.contains("System.loadLibrary(\"native\");"));
    }
}
