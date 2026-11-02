package com.joutvhu.javadec.core.scanner;

import com.joutvhu.javadec.core.model.SecurityAuditResult;
import com.joutvhu.javadec.core.model.SecurityFindingItem;
import com.joutvhu.javadec.core.model.SecurityFindingItem.Severity;
import org.objectweb.asm.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * High-performance bytecode scanner that audits archives and classes for critical security vulnerabilities,
 * risky anti-patterns (Command injection, unsafe deserialization, reflection bypass, weak crypto, hardcoded secrets).
 */
public class BytecodeSecurityAuditor {

    private static final String[] ARCHIVE_CLASS_PREFIXES = {"", "BOOT-INF/classes/", "WEB-INF/classes/"};

    public static SecurityAuditResult auditArchive(File archiveFile, Severity minSeverity, String pathFilter) throws IOException {
        List<SecurityFindingItem> findings = new ArrayList<>();
        String cleanFilter = pathFilter != null ? pathFilter.replace('\\', '/').replaceAll("^/+", "").replace('.', '/') : null;

        try (ZipFile zipFile = new ZipFile(archiveFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.endsWith(".class")) continue;

                String cleanName = name;
                for (String pfx : ARCHIVE_CLASS_PREFIXES) {
                    if (!pfx.isEmpty() && cleanName.startsWith(pfx)) {
                        cleanName = cleanName.substring(pfx.length());
                        break;
                    }
                }

                if (cleanFilter != null && !cleanName.startsWith(cleanFilter) && !cleanName.contains(cleanFilter)) {
                    continue;
                }

                try (InputStream is = zipFile.getInputStream(entry)) {
                    byte[] bytes = is.readAllBytes();
                    auditClassBytecode(bytes, cleanName.substring(0, cleanName.length() - 6), findings);
                } catch (Exception ignored) {}
            }
        }

        if (minSeverity != null) {
            findings.removeIf(f -> f.severity().ordinal() > minSeverity.ordinal());
        }

        return SecurityAuditResult.of(archiveFile.getName(), findings);
    }

    public static SecurityAuditResult auditClass(byte[] bytecode, String className, Severity minSeverity) {
        List<SecurityFindingItem> findings = new ArrayList<>();
        auditClassBytecode(bytecode, className, findings);
        if (minSeverity != null) {
            findings.removeIf(f -> f.severity().ordinal() > minSeverity.ordinal());
        }
        return SecurityAuditResult.of(className != null ? className : "class", findings);
    }

    public static void auditClassBytecode(byte[] bytecode, String className, List<SecurityFindingItem> findings) {
        if (bytecode == null || bytecode.length < 8) return;

        byte[] compatible = bytecode;
        int majorVersion = ((bytecode[6] & 0xFF) << 8) | (bytecode[7] & 0xFF);
        if (majorVersion > 65) {
            compatible = bytecode.clone();
            compatible[6] = 0;
            compatible[7] = 65;
        }

        ClassReader reader = new ClassReader(compatible);
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            private String currentClass = className;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                this.currentClass = name;
            }

            @Override
            public MethodVisitor visitMethod(int access, String methodName, String methodDesc, String signature, String[] exceptions) {
                if (currentClass != null && currentClass.startsWith("com/joutvhu/javadec/core/scanner/BytecodeSecurityAuditor")) {
                    return null;
                }
                return new MethodVisitor(Opcodes.ASM9) {
                    private Integer currentLine;
                    private Object lastLdcValue;

                    @Override
                    public void visitLineNumber(int line, Label start) {
                        this.currentLine = line;
                    }

                    @Override
                    public void visitLdcInsn(Object value) {
                        this.lastLdcValue = value;
                        if (value instanceof String str) {
                            // Check for hardcoded private keys or tokens
                            if (str.contains("BEGIN RSA PRIVATE KEY") || str.contains("BEGIN PRIVATE KEY")
                                    || str.contains("BEGIN EC PRIVATE KEY") || str.contains("BEGIN OPENSSH PRIVATE KEY")) {
                                findings.add(new SecurityFindingItem(
                                        "HARDCODED_PRIVATE_KEY", Severity.CRITICAL, currentClass, methodName, currentLine,
                                        "Hardcoded RSA/EC/SSH private key found in bytecode string constant",
                                        "Found: " + str.substring(0, Math.min(str.length(), 40)) + "..."
                                ));
                            } else if (str.matches(".*AKIA[0-9A-Z]{16}.*")) {
                                findings.add(new SecurityFindingItem(
                                        "HARDCODED_AWS_KEY", Severity.HIGH, currentClass, methodName, currentLine,
                                        "Hardcoded AWS Access Key ID pattern detected",
                                        "Pattern matched AKIA..."
                                ));
                            }
                        }
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        // 1. Command Execution / RCE
                        if ("java/lang/Runtime".equals(owner) && "exec".equals(name)) {
                            findings.add(new SecurityFindingItem(
                                    "COMMAND_INJECTION", Severity.CRITICAL, currentClass, methodName, currentLine,
                                    "Direct operating system command execution via Runtime.getRuntime().exec()",
                                    "Runtime.getRuntime().exec" + descriptor
                            ));
                        } else if ("java/lang/ProcessBuilder".equals(owner) && ("<init>".equals(name) || "command".equals(name))) {
                            findings.add(new SecurityFindingItem(
                                    "COMMAND_INJECTION", Severity.CRITICAL, currentClass, methodName, currentLine,
                                    "Process spawning via ProcessBuilder",
                                    "new ProcessBuilder" + descriptor
                            ));
                        }

                        // 2. Insecure Deserialization
                        if ("java/io/ObjectInputStream".equals(owner) && ("readObject".equals(name) || "readUnshared".equals(name))) {
                            findings.add(new SecurityFindingItem(
                                    "INSECURE_DESERIALIZATION", Severity.HIGH, currentClass, methodName, currentLine,
                                    "Untrusted Java object deserialization via ObjectInputStream." + name + "()",
                                    "ObjectInputStream." + name + "()"
                            ));
                        } else if ("java/beans/XMLDecoder".equals(owner) && "readObject".equals(name)) {
                            findings.add(new SecurityFindingItem(
                                    "INSECURE_DESERIALIZATION", Severity.HIGH, currentClass, methodName, currentLine,
                                    "Arbitrary remote code execution risk via XMLDecoder.readObject()",
                                    "XMLDecoder.readObject()"
                            ));
                        }

                        // 3. Unsafe Memory / Reflection Bypass
                        if ("sun/misc/Unsafe".equals(owner)) {
                            findings.add(new SecurityFindingItem(
                                    "UNSAFE_MEMORY_ACCESS", Severity.HIGH, currentClass, methodName, currentLine,
                                    "Direct off-heap memory manipulation or JVM restriction bypass via sun.misc.Unsafe",
                                    "Unsafe." + name + descriptor
                            ));
                        } else if ("setAccessible".equals(name) && ("java/lang/reflect/AccessibleObject".equals(owner) || "java/lang/reflect/Field".equals(owner) || "java/lang/reflect/Method".equals(owner) || "java/lang/reflect/Constructor".equals(owner))) {
                            findings.add(new SecurityFindingItem(
                                    "REFLECTION_ENCAPSULATION_BYPASS", Severity.MEDIUM, currentClass, methodName, currentLine,
                                    "Bypassing Java encapsulation/private access restrictions via setAccessible(true)",
                                    owner.replace('/', '.') + ".setAccessible"
                            ));
                        }

                        // 4. Native Code / JNI
                        if ("java/lang/System".equals(owner) && ("loadLibrary".equals(name) || "load".equals(name))) {
                            findings.add(new SecurityFindingItem(
                                    "NATIVE_CODE_LOADING", Severity.MEDIUM, currentClass, methodName, currentLine,
                                    "Loading native binary library (.so/.dll/.dylib) into JVM via System." + name + "()",
                                    "System." + name + descriptor
                            ));
                        }

                        // 5. Weak Crypto
                        if ("java/security/MessageDigest".equals(owner) && "getInstance".equals(name)) {
                            if (lastLdcValue instanceof String alg && isWeakDigest(alg)) {
                                findings.add(new SecurityFindingItem(
                                        "WEAK_HASH_ALGORITHM", Severity.MEDIUM, currentClass, methodName, currentLine,
                                        "Collision-vulnerable hash algorithm used in MessageDigest: '" + alg + "'",
                                        "MessageDigest.getInstance(\"" + alg + "\")"
                                ));
                            }
                        } else if ("javax/crypto/Cipher".equals(owner) && "getInstance".equals(name)) {
                            if (lastLdcValue instanceof String alg && isWeakCipher(alg)) {
                                findings.add(new SecurityFindingItem(
                                        "WEAK_CIPHER_ALGORITHM", Severity.HIGH, currentClass, methodName, currentLine,
                                        "Insecure or deprecated cipher/mode used: '" + alg + "'",
                                        "Cipher.getInstance(\"" + alg + "\")"
                                ));
                            }
                        }
                    }
                };
            }
        }, ClassReader.SKIP_FRAMES);
    }

    private static boolean isWeakDigest(String alg) {
        String u = alg.toUpperCase();
        return u.equals("MD5") || u.equals("MD2") || u.equals("MD4") || u.equals("SHA-1") || u.equals("SHA1");
    }

    private static boolean isWeakCipher(String alg) {
        String u = alg.toUpperCase();
        return u.startsWith("DES/") || u.equals("DES") || u.startsWith("DESEDE") || u.startsWith("RC4") || u.startsWith("RC2")
                || u.startsWith("BLOWFISH") || u.contains("/ECB/");
    }
}
