package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.model.ReferenceItem.ReferenceKind;
import com.joutvhu.javadec.core.model.ReferenceSearchResult;
import com.joutvhu.javadec.core.scanner.BytecodeReferenceScanner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class BytecodeReferenceScannerTest {

    @Test
    public void testFindMethodCallsInArchive(@TempDir Path tempDir) throws Exception {
        String serviceSrc = """
                package com.example.service;
                public class AuthService {
                    public boolean authenticate(String user, String pass) {
                        return true;
                    }
                }
                """;

        String clientSrc = """
                package com.example.client;
                import com.example.service.AuthService;
                public class LoginController {
                    private final AuthService auth = new AuthService();
                    public void login() {
                        boolean ok = auth.authenticate("admin", "secret");
                    }
                }
                """;

        Map<String, byte[]> allClasses = TestCompiler.compile(Map.of(
                "com.example.service.AuthService", serviceSrc,
                "com.example.client.LoginController", clientSrc
        ));

        File jar = tempDir.resolve("test-app.jar").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(jar))) {
            for (var entry : allClasses.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey().replace('.', '/') + ".class"));
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }

        // 1. Search by ClassName#methodName
        ReferenceSearchResult res1 = BytecodeReferenceScanner.scanArchive(jar, "com/example/service/AuthService#authenticate", 10, null);
        assertEquals(1, res1.totalFound());
        assertEquals("com/example/client/LoginController", res1.references().get(0).sourceClass());
        assertEquals("login", res1.references().get(0).sourceMethod());
        assertEquals(ReferenceKind.METHOD_CALL, res1.references().get(0).kind());

        // 2. Search by #methodName (wildcard owner)
        ReferenceSearchResult res2 = BytecodeReferenceScanner.scanArchive(jar, "#authenticate", 10, null);
        assertEquals(1, res2.totalFound());
        assertEquals("com/example/client/LoginController", res2.references().get(0).sourceClass());

        // 3. Search by ClassName (find all usages including instantiation)
        ReferenceSearchResult res3 = BytecodeReferenceScanner.scanArchive(jar, "com.example.service.AuthService", 10, null);
        assertTrue(res3.totalFound() >= 2); // instantiation + method call
    }

    @Test
    public void testFindFieldAccessInArchive(@TempDir Path tempDir) throws Exception {
        String configSrc = """
                package com.example;
                public class AppConfig {
                    public static int TIMEOUT = 5000;
                }
                """;

        String workerSrc = """
                package com.example;
                public class Worker {
                    public void run() {
                        int t = AppConfig.TIMEOUT;
                        AppConfig.TIMEOUT = 10000;
                    }
                }
                """;

        Map<String, byte[]> allClasses = TestCompiler.compile(Map.of(
                "com.example.AppConfig", configSrc,
                "com.example.Worker", workerSrc
        ));

        File jar = tempDir.resolve("field-test.jar").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(jar))) {
            for (var entry : allClasses.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey().replace('.', '/') + ".class"));
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }

        ReferenceSearchResult res = BytecodeReferenceScanner.scanArchive(jar, "AppConfig#TIMEOUT", 10, null);
        assertEquals(3, res.totalFound()); // AppConfig.<clinit> (putstatic) + Worker.run (getstatic) + Worker.run (putstatic)

        boolean hasRead = res.references().stream().anyMatch(r -> r.kind() == ReferenceKind.FIELD_READ);
        boolean hasWrite = res.references().stream().anyMatch(r -> r.kind() == ReferenceKind.FIELD_WRITE);
        assertTrue(hasRead);
        assertTrue(hasWrite);

        String summary = res.formattedSummary();
        assertTrue(summary.contains("Worker.run"));
        assertTrue(summary.contains("AppConfig.<clinit>"));
        assertTrue(summary.contains("FIELD_READ"));
        assertTrue(summary.contains("FIELD_WRITE"));
    }

    @Test
    public void testFindMethodReferenceAndClassLiteral(@TempDir Path tempDir) throws Exception {
        String targetSrc = """
                package com.example;
                public class TargetService {
                    public void process(String msg) {}
                }
                """;

        String callerSrc = """
                package com.example;
                import java.util.List;
                public class Invoker {
                    public void execute(List<String> list, TargetService svc) {
                        list.forEach(svc::process);
                        Class<?> c = TargetService.class;
                    }
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile(Map.of(
                "com.example.TargetService", targetSrc,
                "com.example.Invoker", callerSrc
        ));

        File jar = tempDir.resolve("dyn-test.jar").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(jar))) {
            for (var entry : classes.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey().replace('.', '/') + ".class"));
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }

        // 1. Method reference detection (TargetService::process via invokedynamic)
        ReferenceSearchResult resMethod = BytecodeReferenceScanner.scanArchive(jar, "TargetService#process", 10, null);
        assertEquals(1, resMethod.totalFound());
        assertEquals(ReferenceKind.METHOD_CALL, resMethod.references().get(0).kind());
        assertTrue(resMethod.references().get(0).targetDetails().contains("TargetService::process"));

        // 2. Class search (should find method reference + class literal TargetService.class)
        ReferenceSearchResult resClass = BytecodeReferenceScanner.scanArchive(jar, "com.example.TargetService", 10, null);
        assertTrue(resClass.totalFound() >= 2);
        boolean hasLiteral = resClass.references().stream().anyMatch(r -> r.targetDetails().contains("TargetService.class"));
        assertTrue(hasLiteral);
    }

    @Test
    public void testQueryTargetParsing() {
        var t1 = BytecodeReferenceScanner.QueryTarget.parse("com.example.Service#doWork");
        assertEquals("com/example/Service", t1.owner);
        assertEquals("doWork", t1.member);
        assertFalse(t1.isClassSearch());

        var t2 = BytecodeReferenceScanner.QueryTarget.parse("#globalMethod");
        assertNull(t2.owner);
        assertEquals("globalMethod", t2.member);
        assertFalse(t2.isClassSearch());

        var t3 = BytecodeReferenceScanner.QueryTarget.parse("com.example.MyService");
        assertEquals("com/example/MyService", t3.owner);
        assertNull(t3.member);
        assertTrue(t3.isClassSearch());

        var t4 = BytecodeReferenceScanner.QueryTarget.parse("com/example/MyClass.class");
        assertEquals("com/example/MyClass", t4.owner);
        assertNull(t4.member);
        assertTrue(t4.isClassSearch());
    }
}
