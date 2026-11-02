package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.model.ClassOutline;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ClassOutlineTest {

    @Test
    public void testClassSkeleton() {
        ClassOutline outline = new ClassOutline(
                "com/example/UserService",
                "com.example",
                "public",
                "class",
                "com/example/BaseService",
                List.of("com/example/IUserService"),
                List.of("org.springframework.stereotype.Service"),
                List.of(),
                List.of(),
                List.of(new ClassOutline.FieldOutline("userRepo", "com.example.UserRepo", "private final", List.of("org.springframework.beans.factory.annotation.Autowired"))),
                List.of(
                        new ClassOutline.MethodOutline("<init>", "void", List.of(new ClassOutline.ParameterOutline("com.example.UserRepo", "userRepo")), List.of(), "public", List.of()),
                        new ClassOutline.MethodOutline("findUser", "com.example.User", List.of(new ClassOutline.ParameterOutline("java.lang.String", "id")), List.of("java.io.IOException"), "public", List.of("java.lang.Override")),
                        new ClassOutline.MethodOutline("<clinit>", "void", List.of(), List.of(), "static", List.of()) // should be skipped
                ),
                65
        );

        String skel = outline.toSkeletonSource();
        assertTrue(skel.contains("package com.example;"));
        assertTrue(skel.contains("@org.springframework.stereotype.Service"));
        assertTrue(skel.contains("public class UserService extends BaseService implements IUserService"));
        assertTrue(skel.contains("@org.springframework.beans.factory.annotation.Autowired"));
        assertTrue(skel.contains("private final com.example.UserRepo userRepo;"));
        assertTrue(skel.contains("public UserService(com.example.UserRepo userRepo);")); // constructor format
        assertTrue(skel.contains("@java.lang.Override"));
        assertTrue(skel.contains("public com.example.User findUser(java.lang.String id) throws IOException;"));
        assertFalse(skel.contains("<clinit>")); // <clinit> must be skipped
    }

    @Test
    public void testInterfaceSkeleton() {
        ClassOutline outline = new ClassOutline(
                "com/example/CustomList",
                "com.example",
                "public",
                "interface",
                "java/lang/Object",
                List.of("java/util/List", "java/util/RandomAccess"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new ClassOutline.MethodOutline("customMethod", "void", List.of(), List.of(), "public abstract", List.of())),
                65
        );

        String skel = outline.toSkeletonSource();
        // An interface extending another interface MUST use 'extends', NOT 'implements'!
        assertTrue(skel.contains("public interface CustomList extends List, RandomAccess"));
        assertFalse(skel.contains("implements"));
        assertFalse(skel.contains("extends Object"));
    }

    @Test
    public void testRecordSkeleton() {
        ClassOutline outline = new ClassOutline(
                "com/example/UserRecord",
                "com.example",
                "public final",
                "record",
                "java/lang/Record",
                List.of("java/io/Serializable"),
                List.of(),
                List.of(
                        new ClassOutline.RecordComponentOutline("java.lang.String", "username"),
                        new ClassOutline.RecordComponentOutline("int", "age")
                ),
                List.of(),
                List.of(),
                List.of(),
                65
        );

        String skel = outline.toSkeletonSource();
        assertTrue(skel.contains("public final record UserRecord(java.lang.String username, int age) implements Serializable"));
        assertFalse(skel.contains("extends Record"));
    }

    @Test
    public void testEmptyRecordSkeleton() {
        ClassOutline outline = new ClassOutline(
                "com/example/EmptyRecord",
                "com.example",
                "public final",
                "record",
                "java/lang/Record",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                65
        );

        String skel = outline.toSkeletonSource();
        assertTrue(skel.contains("public final record EmptyRecord()"));
    }

    @Test
    public void testEnumSkeleton() {
        ClassOutline outline = new ClassOutline(
                "com/example/Status",
                "com.example",
                "public",
                "enum",
                "java/lang/Enum",
                List.of("com/example/HasCode"),
                List.of(),
                List.of(),
                List.of("ACTIVE", "INACTIVE", "PENDING"),
                List.of(new ClassOutline.FieldOutline("code", "int", "private final", List.of())),
                List.of(),
                65
        );

        String skel = outline.toSkeletonSource();
        assertTrue(skel.contains("public enum Status implements HasCode"));
        assertFalse(skel.contains("extends Enum")); // MUST NOT have 'extends Enum'
        assertTrue(skel.contains("ACTIVE, INACTIVE, PENDING;")); // enum constants
        assertTrue(skel.contains("private final int code;"));
    }

    @Test
    public void testAsmExtractorWithRealCompiledClass() {
        String source = """
                package com.example;
                public class RealService {
                    private volatile int counter;
                    public synchronized void increment() {
                        counter++;
                    }
                }
                """;
        java.util.Map<String, byte[]> classes = TestCompiler.compile("com.example.RealService", source);
        byte[] bytes = classes.get("com.example.RealService");
        assertNotNull(bytes);

        com.joutvhu.javadec.core.model.ClassOutline outline = com.joutvhu.javadec.core.outline.AsmClassOutlineExtractor.extract(bytes);
        String skeleton = outline.toSkeletonSource();

        // Class modifier MUST be "public class", NOT "public synchronized class"
        assertTrue(skeleton.contains("public class RealService"));
        assertFalse(skeleton.contains("synchronized class"));

        // Field modifier MUST include "volatile"
        assertTrue(skeleton.contains("private volatile int counter;"));

        // Method modifier MUST include "synchronized"
        assertTrue(skeleton.contains("public synchronized void increment();"));
    }
}
