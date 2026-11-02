package com.joutvhu.javadec.core.scanner;

import com.joutvhu.javadec.core.model.ConstantStringItem;
import com.joutvhu.javadec.core.model.ConstantStringResult;
import org.objectweb.asm.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Ultra-fast scanner to extract hardcoded string literals (URLs, SQL, tokens, error messages)
 * directly from bytecode constant pool and LDC instructions without decompilation.
 */
public class ConstantStringExtractor {

    private static final String[] ARCHIVE_CLASS_PREFIXES = {"", "BOOT-INF/classes/", "WEB-INF/classes/"};

    public static ConstantStringResult extractFromArchive(File archiveFile, String pathFilter, String textFilter, Integer minLength, Integer limit) throws IOException {
        int maxResults = (limit != null && limit > 0) ? limit : 200;
        int minLen = (minLength != null && minLength > 0) ? minLength : 3;
        String lowerFilter = textFilter != null ? textFilter.trim().toLowerCase() : null;
        String cleanPathFilter = pathFilter != null ? pathFilter.replace('\\', '/').replaceAll("^/+", "").replace('.', '/') : null;

        List<ConstantStringItem> items = new ArrayList<>();

        try (ZipFile zipFile = new ZipFile(archiveFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements() && items.size() < maxResults) {
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

                if (cleanPathFilter != null && !cleanName.startsWith(cleanPathFilter) && !cleanName.contains(cleanPathFilter)) {
                    continue;
                }

                try (InputStream is = zipFile.getInputStream(entry)) {
                    byte[] bytecode = is.readAllBytes();
                    extractFromBytecode(bytecode, cleanName.substring(0, cleanName.length() - 6), lowerFilter, minLen, items, maxResults);
                } catch (Exception ignored) {}
            }
        }

        return ConstantStringResult.of(archiveFile.getName(), textFilter, items);
    }

    public static ConstantStringResult extractFromClassBytes(byte[] bytecode, String className, String textFilter, Integer minLength, Integer limit) {
        int maxResults = (limit != null && limit > 0) ? limit : 200;
        int minLen = (minLength != null && minLength > 0) ? minLength : 3;
        String lowerFilter = textFilter != null ? textFilter.trim().toLowerCase() : null;

        List<ConstantStringItem> items = new ArrayList<>();
        extractFromBytecode(bytecode, className, lowerFilter, minLen, items, maxResults);
        return ConstantStringResult.of(className != null ? className : "class", textFilter, items);
    }

    public static void extractFromBytecode(byte[] bytecode, String className, String textFilter, int minLength, List<ConstantStringItem> results, int limit) {
        if (bytecode == null || bytecode.length < 8 || results.size() >= limit) return;

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
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                if (results.size() >= limit) return null;
                if (value instanceof String str) {
                    checkAndAdd(currentClass, "field " + name, null, str, textFilter, minLength, results, limit);
                }
                return null;
            }

            @Override
            public MethodVisitor visitMethod(int access, String methodName, String methodDesc, String signature, String[] exceptions) {
                if (results.size() >= limit) return null;
                return new MethodVisitor(Opcodes.ASM9) {
                    private Integer currentLine;

                    @Override
                    public void visitLineNumber(int line, Label start) {
                        this.currentLine = line;
                    }

                    @Override
                    public void visitLdcInsn(Object value) {
                        if (results.size() >= limit) return;
                        if (value instanceof String str) {
                            checkAndAdd(currentClass, methodName, currentLine, str, textFilter, minLength, results, limit);
                        }
                    }
                };
            }
        }, ClassReader.SKIP_FRAMES);
    }

    private static void checkAndAdd(String cls, String method, Integer line, String str, String filter, int minLen, List<ConstantStringItem> results, int limit) {
        if (str == null || str.length() < minLen) return;
        if (filter != null && !str.toLowerCase().contains(filter)) return;
        results.add(new ConstantStringItem(cls, method, line, str));
    }
}
