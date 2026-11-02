package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.loader.JarSignatureVerifier;
import com.joutvhu.javadec.core.model.JarSignatureResult;
import com.joutvhu.javadec.core.model.JarSignatureResult.SignatureStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class JarSignatureVerifierTest {

    @Test
    public void testVerifyUnsignedJar(@TempDir Path tempDir) throws Exception {
        File unsignedJar = tempDir.resolve("unsigned.jar").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(unsignedJar))) {
            zos.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            zos.write("Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        JarSignatureResult result = JarSignatureVerifier.verify(unsignedJar);
        assertNotNull(result);
        assertEquals(SignatureStatus.UNSIGNED, result.status());
        assertTrue(result.formattedSummary().contains("UNSIGNED"));
    }
}
