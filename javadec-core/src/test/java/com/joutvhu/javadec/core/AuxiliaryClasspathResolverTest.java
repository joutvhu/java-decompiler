package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.api.DecompileOptions;
import com.joutvhu.javadec.core.api.JavaDecompiler;
import com.joutvhu.javadec.core.loader.AuxiliaryClasspathResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class AuxiliaryClasspathResolverTest {

    @Test
    public void testResolveManualClasspath(@TempDir Path tempDir) throws Exception {
        File dummyJar = tempDir.resolve("lib.jar").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(dummyJar))) {
            zos.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            zos.write("Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        // 1. Direct JAR path
        List<File> jars1 = AuxiliaryClasspathResolver.resolve(null, dummyJar.getAbsolutePath(), false);
        assertEquals(1, jars1.size());
        assertEquals(dummyJar.getAbsolutePath(), jars1.get(0).getAbsolutePath());

        // 2. Directory containing JAR
        List<File> jars2 = AuxiliaryClasspathResolver.resolve(null, tempDir.toFile().getAbsolutePath(), false);
        assertEquals(1, jars2.size());
    }

    @Test
    public void testAutoDetectFromCompanionPom(@TempDir Path tempDir) throws Exception {
        File jar = tempDir.resolve("my-artifact-1.0.jar").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(jar))) {
            zos.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            zos.write("Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        // Companion POM referencing org.ow2.asm:asm:9.7 (present in local repo)
        File pom = tempDir.resolve("my-artifact-1.0.pom").toFile();
        String pomXml = """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>my-artifact</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.ow2.asm</groupId>
                            <artifactId>asm</artifactId>
                            <version>9.7</version>
                        </dependency>
                    </dependencies>
                </project>
                """;
        java.nio.file.Files.writeString(pom.toPath(), pomXml);

        List<File> detected = AuxiliaryClasspathResolver.resolve(jar, null, true);
        assertNotNull(detected);
        boolean hasAsm = detected.stream().anyMatch(f -> f.getName().startsWith("asm-9.7.jar"));
        assertTrue(hasAsm, "Should auto-detect asm-9.7.jar from companion pom");
    }

    @Test
    public void testDecompileWithExternalClasspathResolution(@TempDir Path tempDir) throws Exception {
        // Library class
        String baseSrc = """
                package com.example.base;
                public class BaseEntity<T> {
                    private T id;
                    public T getId() { return id; }
                    public void setId(T id) { this.id = id; }
                }
                """;

        // Application class inheriting from library class
        String subSrc = """
                package com.example.app;
                import com.example.base.BaseEntity;
                public class UserEntity extends BaseEntity<String> {
                    public String getUserIdentifier() {
                        return getId();
                    }
                }
                """;

        Map<String, byte[]> baseClasses = TestCompiler.compile("com.example.base.BaseEntity", baseSrc);
        File libJar = tempDir.resolve("base-lib.jar").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(libJar))) {
            for (var entry : baseClasses.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey().replace('.', '/') + ".class"));
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }

        Map<String, byte[]> subClasses = TestCompiler.compile(Map.of(
                "com.example.base.BaseEntity", baseSrc,
                "com.example.app.UserEntity", subSrc
        ));
        File appJar = tempDir.resolve("app.jar").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(appJar))) {
            zos.putNextEntry(new ZipEntry("com/example/app/UserEntity.class"));
            zos.write(subClasses.get("com.example.app.UserEntity"));
            zos.closeEntry();
        }

        // Decompile UserEntity passing base-lib.jar as auxiliary classpath
        JavaDecompiler decompiler = new JavaDecompiler();
        DecompileOptions options = DecompileOptions.builder()
                .extraClasspath(libJar.getAbsolutePath())
                .autoDetectDependencies(false)
                .build();

        String decompiled = decompiler.decompileArchiveClass(appJar, "com/example/app/UserEntity.class", options);
        assertNotNull(decompiled);
        assertTrue(decompiled.contains("class UserEntity"));
        assertTrue(decompiled.contains("BaseEntity"));
        assertTrue(decompiled.contains("getUserIdentifier"));
    }

    @Test
    public void testResolveBootInfLib(@TempDir Path tempDir) throws Exception {
        // Create an embedded library jar
        File embeddedJar = tempDir.resolve("embedded.jar").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(embeddedJar))) {
            zos.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            zos.write("Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        // Create a Spring Boot fat jar containing BOOT-INF/lib/embedded.jar
        File fatJar = tempDir.resolve("spring-boot-app.jar").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(fatJar))) {
            zos.putNextEntry(new ZipEntry("BOOT-INF/lib/embedded.jar"));
            zos.write(java.nio.file.Files.readAllBytes(embeddedJar.toPath()));
            zos.closeEntry();
        }

        List<File> resolved = AuxiliaryClasspathResolver.resolve(fatJar, null, true);
        assertNotNull(resolved);
        boolean hasEmbedded = resolved.stream().anyMatch(f -> f.getName().equals("embedded.jar"));
        assertTrue(hasEmbedded, "Should resolve embedded libraries from BOOT-INF/lib");
    }
}
