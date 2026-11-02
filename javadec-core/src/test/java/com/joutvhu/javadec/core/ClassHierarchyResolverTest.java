package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.model.ClassHierarchyResult;
import com.joutvhu.javadec.core.scanner.ClassHierarchyResolver;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ClassHierarchyResolverTest {

    @Test
    public void testResolveInheritanceChain() {
        String baseSrc = """
                package com.example;
                public interface Identifiable {}
                """;

        String midSrc = """
                package com.example;
                import java.io.Serializable;
                public class ParentEntity implements Identifiable, Serializable {}
                """;

        String childSrc = """
                package com.example;
                public class ChildEntity extends ParentEntity {}
                """;

        Map<String, byte[]> classes = TestCompiler.compile(Map.of(
                "com.example.Identifiable", baseSrc,
                "com.example.ParentEntity", midSrc,
                "com.example.ChildEntity", childSrc
        ));

        ClassHierarchyResult result = ClassHierarchyResolver.resolve(null, classes.get("com.example.ChildEntity"), "com.example.ChildEntity");
        assertNotNull(result);
        assertEquals("com.example.ChildEntity", result.className());
        assertTrue(result.superclassChain().contains("com.example.ChildEntity"));
        assertTrue(result.superclassChain().contains("com.example.ParentEntity"));

        String formatted = result.formattedHierarchy();
        assertTrue(formatted.contains("ChildEntity"));
        assertTrue(formatted.contains("ParentEntity"));
    }

    @Test
    public void testResolveRecursiveInterfaces() {
        String listSrc = """
                package com.example;
                import java.util.ArrayList;
                public class MyCustomList extends ArrayList<String> {}
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.MyCustomList", listSrc);
        ClassHierarchyResult result = ClassHierarchyResolver.resolve(null, classes.get("com.example.MyCustomList"), "com.example.MyCustomList");
        assertNotNull(result);
        // Direct interface from ArrayList is List, RandomAccess, Cloneable, Serializable
        // Indirect super-interface of List is Collection, and Iterable
        assertTrue(result.implementedInterfaces().containsKey("java.lang.Iterable")
                || result.implementedInterfaces().containsKey("java.util.Collection"),
                "Should recursively resolve super-interfaces like Collection or Iterable");
    }
}
