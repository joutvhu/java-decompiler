package com.joutvhu.javadec.core.model;

import java.util.List;

/**
 * Result of verifying digital signatures, certificates, and entry digests of a JAR archive.
 */
public record JarSignatureResult(
        String archiveName,
        SignatureStatus status,
        List<SignerInfo> signers,
        String formattedSummary
) {
    public enum SignatureStatus {
        SIGNED_VALID,
        UNSIGNED,
        TAMPERED
    }

    public record SignerInfo(
            String subject,
            String issuer,
            String algorithm,
            String validUntil
    ) {}
}
