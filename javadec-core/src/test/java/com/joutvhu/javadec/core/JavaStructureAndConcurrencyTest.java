package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.api.DecompileOptions;
import com.joutvhu.javadec.core.api.JavaDecompiler;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JavaStructureAndConcurrencyTest {

    private final JavaDecompiler decompiler = new JavaDecompiler();

    @Test
    public void testSynchronizedAndVolatile() {
        String source = """
                package com.example.concurrency;
                public class ConcurrencyDemo {
                    private volatile boolean flag = false;
                    private final Object lock = new Object();

                    public synchronized void syncMethod() {
                        this.flag = true;
                    }

                    public void syncBlock() {
                        synchronized (this.lock) {
                            System.out.println("Locked");
                        }
                    }
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.concurrency.ConcurrencyDemo", source);
        String decompiled = decompiler.decompile("com/example/concurrency/ConcurrencyDemo", classes.get("com.example.concurrency.ConcurrencyDemo"), DecompileOptions.defaults());
        assertNotNull(decompiled);
        assertTrue(decompiled.contains("volatile boolean flag"));
        assertTrue(decompiled.contains("synchronized void syncMethod()"));
        assertTrue(decompiled.contains("synchronized ("));
    }

    @Test
    public void testBoundedGenerics() {
        String source = """
                package com.example.generics;
                import java.util.List;
                public class GenericRepo<T extends Comparable<T>, ID> {
                    private ID id;
                    public T findMax(List<T> items) {
                        return items.isEmpty() ? null : items.get(0);
                    }
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.generics.GenericRepo", source);
        String decompiled = decompiler.decompile("com/example/generics/GenericRepo", classes.get("com.example.generics.GenericRepo"), DecompileOptions.defaults());
        assertNotNull(decompiled);
        assertTrue(decompiled.contains("GenericRepo<T extends Comparable<T>, ID>") || decompiled.contains("GenericRepo<T, ID>"));
        assertTrue(decompiled.contains("findMax(List<T> items)"));
    }

    @Test
    public void testInnerAndAnonymousClasses() {
        String source = """
                package com.example.structures;
                public class OuterDemo {
                    public static class StaticNested {
                        public String greet() { return "nested"; }
                    }

                    public Runnable createAction() {
                        return new Runnable() {
                            @Override
                            public void run() {
                                System.out.println("anonymous");
                            }
                        };
                    }
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.structures.OuterDemo", source);
        // Pass classes map to resolve inner and anonymous classes
        String decompiled = decompiler.decompile("com/example/structures/OuterDemo", classes.get("com.example.structures.OuterDemo"), classes, DecompileOptions.defaults());
        assertNotNull(decompiled);
        assertTrue(decompiled.contains("StaticNested"));
        assertTrue(decompiled.contains("Runnable"));
    }
}
