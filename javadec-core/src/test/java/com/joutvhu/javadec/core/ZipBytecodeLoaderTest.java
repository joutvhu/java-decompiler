package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.loader.ZipBytecodeLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class ZipBytecodeLoaderTest {

    @Test
    public void testCaseInsensitiveListClasses(@TempDir Path tempDir) throws Exception {
        File zip = tempDir.resolve("test.jar").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry("com/example/MyService.class"));
            zos.write(new byte[]{1, 2, 3});
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("com/example/UserController.class"));
            zos.write(new byte[]{1, 2, 3});
            zos.closeEntry();
        }

        // Search with lowercase 'myservice'
        List<String> found = ZipBytecodeLoader.listClasses(zip, "myservice");
        assertEquals(1, found.size());
        assertEquals("com/example/MyService", found.get(0));

        // Search with glob '*user*'
        List<String> foundUser = ZipBytecodeLoader.listClasses(zip, "*user*");
        assertEquals(1, foundUser.size());
        assertEquals("com/example/UserController", foundUser.get(0));
    }

    @Test
    public void testSpringBootPrefixResolution(@TempDir Path tempDir) throws Exception {
        File fatJar = tempDir.resolve("spring-app.jar").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(fatJar))) {
            zos.putNextEntry(new ZipEntry("BOOT-INF/classes/com/company/MyService.class"));
            zos.write(new byte[]{10, 20, 30});
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("BOOT-INF/classes/application.yml"));
            zos.write("server:\n  port: 8080\n".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        // 1. Resolve class without BOOT-INF/classes prefix
        byte[] bytes1 = ZipBytecodeLoader.loadClassBytes(fatJar, "com/company/MyService.class");
        assertArrayEquals(new byte[]{10, 20, 30}, bytes1);

        // 2. Resolve class WITH BOOT-INF/classes prefix
        byte[] bytes2 = ZipBytecodeLoader.loadClassBytes(fatJar, "BOOT-INF/classes/com/company/MyService.class");
        assertArrayEquals(new byte[]{10, 20, 30}, bytes2);

        // 3. Resolve resource without prefix
        String yml1 = ZipBytecodeLoader.readResourceText(fatJar, "application.yml");
        assertTrue(yml1.contains("8080"));

        // 4. Resolve resource WITH prefix
        String yml2 = ZipBytecodeLoader.readResourceText(fatJar, "BOOT-INF/classes/application.yml");
        assertTrue(yml2.contains("8080"));
    }

    @Test
    public void testBinaryAssetGuardInReadResource(@TempDir Path tempDir) throws Exception {
        File jar = tempDir.resolve("assets.jar").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(jar))) {
            zos.putNextEntry(new ZipEntry("icons/logo.png"));
            zos.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', 1, 2, 3});
            zos.closeEntry();
        }

        String result = ZipBytecodeLoader.readResourceText(jar, "icons/logo.png");
        assertTrue(result.contains("[Binary asset: icons/logo.png"));
        assertTrue(result.contains("Not a text resource"));
    }

    @Test
    public void testUtf8BomStrippingInReadResource(@TempDir Path tempDir) throws Exception {
        File jar = tempDir.resolve("bom.jar").toFile();
        byte[] contentWithBom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'k', 'e', 'y', '=', 'v', 'a', 'l'};
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(jar))) {
            zos.putNextEntry(new ZipEntry("app.properties"));
            zos.write(contentWithBom);
            zos.closeEntry();
        }

        String result = ZipBytecodeLoader.readResourceText(jar, "app.properties");
        assertEquals("key=val", result);
        assertFalse(result.startsWith("\uFEFF"));
    }

    @Test
    public void testZipSlipSecurityCheck(@TempDir Path tempDir) throws Exception {
        File maliciousZip = tempDir.resolve("malicious.jar").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(maliciousZip))) {
            zos.putNextEntry(new ZipEntry("../../outside.txt"));
            zos.write("attack".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        File extractTarget = tempDir.resolve("extract").toFile();
        assertThrows(IOException.class, () -> {
            ZipBytecodeLoader.extractArchive(maliciousZip, extractTarget, null);
        });
    }
}
