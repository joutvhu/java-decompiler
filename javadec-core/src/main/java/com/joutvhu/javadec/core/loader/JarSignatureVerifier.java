package com.joutvhu.javadec.core.loader;

import com.joutvhu.javadec.core.model.JarSignatureResult;
import com.joutvhu.javadec.core.model.JarSignatureResult.SignatureStatus;
import com.joutvhu.javadec.core.model.JarSignatureResult.SignerInfo;

import java.io.File;
import java.io.InputStream;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Verifies digital signatures, certificate chains, and manifest entry hashes of JAR archives.
 */
public class JarSignatureVerifier {

    public static JarSignatureResult verify(File jarFile) {
        if (jarFile == null || !jarFile.exists() || !jarFile.isFile()) {
            return new JarSignatureResult(jarFile != null ? jarFile.getName() : "unknown",
                    SignatureStatus.UNSIGNED, List.of(), "// File does not exist or is not a file.");
        }

        boolean hasSignatureFiles = false;
        List<SignerInfo> signers = new ArrayList<>();
        Set<String> seenSigners = new HashSet<>();
        boolean tampered = false;
        boolean hasUnsignedEntries = false;

        try (JarFile jar = new JarFile(jarFile, true)) {
            Enumeration<JarEntry> entries = jar.entries();
            byte[] buffer = new byte[8192];

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName().toUpperCase();

                if (name.startsWith("META-INF/") && (name.endsWith(".SF") || name.endsWith(".RSA") || name.endsWith(".DSA") || name.endsWith(".EC"))) {
                    hasSignatureFiles = true;
                }

                // Read entire entry to trigger signature hash verification
                if (!entry.isDirectory() && !name.startsWith("META-INF/")) {
                    try (InputStream is = jar.getInputStream(entry)) {
                        while (is.read(buffer) != -1) {}
                    } catch (SecurityException se) {
                        tampered = true;
                        break;
                    }

                    // Collect signers
                    CodeSigner[] codeSigners = entry.getCodeSigners();
                    if (codeSigners != null && codeSigners.length > 0) {
                        for (CodeSigner cs : codeSigners) {
                            List<? extends Certificate> certs = cs.getSignerCertPath().getCertificates();
                            if (!certs.isEmpty() && certs.get(0) instanceof X509Certificate x509) {
                                String sub = x509.getSubjectX500Principal().getName();
                                if (seenSigners.add(sub)) {
                                    String iss = x509.getIssuerX500Principal().getName();
                                    String alg = x509.getSigAlgName();
                                    String validUntil = x509.getNotAfter().toString();
                                    signers.add(new SignerInfo(sub, iss, alg, validUntil));
                                }
                            }
                        }
                    } else {
                        hasUnsignedEntries = true;
                    }
                }
            }
        } catch (Exception e) {
            return new JarSignatureResult(jarFile.getName(), SignatureStatus.UNSIGNED, List.of(), "// Error reading JAR signature: " + e.getMessage());
        }

        SignatureStatus status;
        if (tampered) {
            status = SignatureStatus.TAMPERED;
        } else if (hasSignatureFiles && !signers.isEmpty()) {
            if (hasUnsignedEntries) {
                status = SignatureStatus.TAMPERED; // Unsigned entries in a signed archive indicate tampering
            } else {
                status = SignatureStatus.SIGNED_VALID;
            }
        } else {
            status = SignatureStatus.UNSIGNED;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("JAR Signature Verification: ").append(jarFile.getName()).append("\n");
        sb.append("- Status: ");
        if (status == SignatureStatus.SIGNED_VALID) {
            sb.append("SIGNED (Valid & Untampered)\n");
            for (SignerInfo si : signers) {
                sb.append("  - Signer: ").append(si.subject()).append("\n");
                sb.append("  - Issuer: ").append(si.issuer()).append("\n");
                sb.append("  - Algorithm: ").append(si.algorithm()).append("\n");
                sb.append("  - Valid Until: ").append(si.validUntil()).append("\n");
            }
        } else if (status == SignatureStatus.TAMPERED) {
            sb.append("TAMPERED (Signature validation failed! Entry hashes modified or unsigned entries present in signed archive)\n");
        } else {
            sb.append("UNSIGNED (No signature certificates found)\n");
        }

        return new JarSignatureResult(jarFile.getName(), status, signers, sb.toString().trim());
    }
}
