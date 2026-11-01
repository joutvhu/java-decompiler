package com.joutvhu.javadec.core.scanner;

import com.joutvhu.javadec.core.model.ReferenceItem;
import com.joutvhu.javadec.core.model.ReferenceItem.ReferenceKind;
import com.joutvhu.javadec.core.model.ReferenceSearchResult;
import org.objectweb.asm.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Ultra-fast bytecode scanner powered by ASM to find usages/references of classes, methods, or fields.
 * Scans bytecode directly in-memory from JAR/ZIP archives or directories with zero decompilation overhead.
 */
public class BytecodeReferenceScanner {

    private static final String[] ARCHIVE_CLASS_PREFIXES = {"", "BOOT-INF/classes/", "WEB-INF/classes/"};

    /**
     * Scans a JAR or ZIP archive for references matching the query.
     */
    public static ReferenceSearchResult scanArchive(File archiveFile, String query, Integer limit, String pathFilter) throws IOException {
        int maxResults = (limit != null && limit > 0) ? limit : 100;
        QueryTarget target = QueryTarget.parse(query);
        List<ReferenceItem> references = new ArrayList<>();

        String cleanFilter = pathFilter != null ? pathFilter.replace('\\', '/').replaceAll("^/+", "").replace('.', '/') : null;

        try (ZipFile zipFile = new ZipFile(archiveFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements() && references.size() < maxResults) {
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
                    byte[] bytecode = is.readAllBytes();
                    scanBytecode(bytecode, target, references, maxResults);
                } catch (Exception ignored) {}
            }
        }

        return ReferenceSearchResult.of(query, archiveFile.getName(), references);
    }

    /**
     * Scans a directory of compiled .class files for references matching the query.
     */
    public static ReferenceSearchResult scanDirectory(File directory, String query, Integer limit) throws IOException {
        int maxResults = (limit != null && limit > 0) ? limit : 100;
        QueryTarget target = QueryTarget.parse(query);
        List<ReferenceItem> references = new ArrayList<>();

        try (var stream = Files.walk(directory.toPath())) {
            for (Path path : (Iterable<Path>) stream::iterator) {
                if (references.size() >= maxResults) break;
                if (Files.isRegularFile(path) && path.toString().endsWith(".class")) {
                    try {
                        byte[] bytecode = Files.readAllBytes(path);
                        scanBytecode(bytecode, target, references, maxResults);
                    } catch (Exception ignored) {}
                }
            }
        }

        return ReferenceSearchResult.of(query, directory.getName(), references);
    }

    /**
     * Scans a single class bytecode array for references to the target.
     */
    public static void scanBytecode(byte[] bytecode, QueryTarget target, List<ReferenceItem> results, int limit) {
        if (bytecode == null || bytecode.length < 8 || results.size() >= limit) return;

        // Ensure compatibility with newer bytecode versions (Java 22-27+) for ASM reader
        byte[] compatibleBytecode = bytecode;
        int majorVersion = ((bytecode[6] & 0xFF) << 8) | (bytecode[7] & 0xFF);
        if (majorVersion > 65) {
            compatibleBytecode = bytecode.clone();
            compatibleBytecode[6] = 0;
            compatibleBytecode[7] = 65;
        }

        ClassReader reader = new ClassReader(compatibleBytecode);
        reader.accept(new ReferenceVisitor(target, results, limit), ClassReader.SKIP_FRAMES);
    }

    private static class ReferenceVisitor extends ClassVisitor {
        private final QueryTarget target;
        private final List<ReferenceItem> results;
        private final int limit;
        private String currentClassName;

        public ReferenceVisitor(QueryTarget target, List<ReferenceItem> results, int limit) {
            super(Opcodes.ASM9);
            this.target = target;
            this.results = results;
            this.limit = limit;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.currentClassName = name;

            if (target.isClassSearch() && results.size() < limit) {
                if (superName != null && target.matchesClass(superName)) {
                    results.add(new ReferenceItem(name, null, null, null, ReferenceKind.EXTENDS, superName));
                }
                if (interfaces != null) {
                    for (String iface : interfaces) {
                        if (target.matchesClass(iface)) {
                            results.add(new ReferenceItem(name, null, null, null, ReferenceKind.IMPLEMENTS, iface));
                        }
                    }
                }
            }
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            if (target.isClassSearch() && results.size() < limit) {
                if (descriptorContainsClass(descriptor, target.owner)) {
                    results.add(new ReferenceItem(currentClassName, name, descriptor, null, ReferenceKind.TYPE_USAGE, "Field type: " + descriptor));
                }
            }
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String methodName, String methodDesc, String signature, String[] exceptions) {
            if (results.size() >= limit) return null;

            if (target.isClassSearch()) {
                if (descriptorContainsClass(methodDesc, target.owner)) {
                    results.add(new ReferenceItem(currentClassName, methodName, methodDesc, null, ReferenceKind.TYPE_USAGE, "Signature: " + methodDesc));
                }
                if (exceptions != null) {
                    for (String exc : exceptions) {
                        if (target.matchesClass(exc)) {
                            results.add(new ReferenceItem(currentClassName, methodName, methodDesc, null, ReferenceKind.TYPE_USAGE, "Throws: " + exc));
                        }
                    }
                }
            }

            return new MethodReferenceVisitor(currentClassName, methodName, methodDesc, target, results, limit);
        }
    }

    private static class MethodReferenceVisitor extends MethodVisitor {
        private final String sourceClass;
        private final String sourceMethod;
        private final String sourceMethodDesc;
        private final QueryTarget target;
        private final List<ReferenceItem> results;
        private final int limit;
        private Integer currentLine;

        public MethodReferenceVisitor(String sourceClass, String sourceMethod, String sourceMethodDesc, QueryTarget target, List<ReferenceItem> results, int limit) {
            super(Opcodes.ASM9);
            this.sourceClass = sourceClass;
            this.sourceMethod = sourceMethod;
            this.sourceMethodDesc = sourceMethodDesc;
            this.target = target;
            this.results = results;
            this.limit = limit;
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            this.currentLine = line;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            if (results.size() >= limit) return;

            if (target.matchesMethod(owner, name)) {
                ReferenceKind kind = name.equals("<init>") ? ReferenceKind.INSTANTIATION : ReferenceKind.METHOD_CALL;
                String targetDisplay = owner.replace('/', '.') + "." + name + descriptor;
                results.add(new ReferenceItem(sourceClass, sourceMethod, sourceMethodDesc, currentLine, kind, targetDisplay));
            } else if (target.isClassSearch() && target.matchesClass(owner)) {
                ReferenceKind kind = name.equals("<init>") ? ReferenceKind.INSTANTIATION : ReferenceKind.METHOD_CALL;
                String targetDisplay = owner.replace('/', '.') + "." + name + descriptor;
                results.add(new ReferenceItem(sourceClass, sourceMethod, sourceMethodDesc, currentLine, kind, targetDisplay));
            }
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            if (results.size() >= limit) return;

            if (target.matchesField(owner, name)) {
                boolean isWrite = (opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC);
                ReferenceKind kind = isWrite ? ReferenceKind.FIELD_WRITE : ReferenceKind.FIELD_READ;
                String targetDisplay = owner.replace('/', '.') + "." + name + " : " + descriptor;
                results.add(new ReferenceItem(sourceClass, sourceMethod, sourceMethodDesc, currentLine, kind, targetDisplay));
            } else if (target.isClassSearch() && target.matchesClass(owner)) {
                boolean isWrite = (opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC);
                ReferenceKind kind = isWrite ? ReferenceKind.FIELD_WRITE : ReferenceKind.FIELD_READ;
                String targetDisplay = owner.replace('/', '.') + "." + name + " : " + descriptor;
                results.add(new ReferenceItem(sourceClass, sourceMethod, sourceMethodDesc, currentLine, kind, targetDisplay));
            }
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            if (results.size() >= limit) return;

            if (target.isClassSearch() && target.matchesClass(type)) {
                ReferenceKind kind = switch (opcode) {
                    case Opcodes.NEW -> ReferenceKind.INSTANTIATION;
                    case Opcodes.CHECKCAST, Opcodes.INSTANCEOF -> ReferenceKind.TYPE_CHECK;
                    default -> ReferenceKind.TYPE_USAGE;
                };
                results.add(new ReferenceItem(sourceClass, sourceMethod, sourceMethodDesc, currentLine, kind, type.replace('/', '.')));
            }
        }
    }

    private static boolean descriptorContainsClass(String descriptor, String internalClass) {
        if (descriptor == null || internalClass == null) return false;
        return descriptor.contains("L" + internalClass + ";") || descriptor.contains("L" + internalClass.replace('.', '/') + ";");
    }

    public static class QueryTarget {
        public final String owner;   // e.g. "com/example/MyService" (or null if wild)
        public final String member;  // e.g. "execute" (or null if class search)

        private QueryTarget(String owner, String member) {
            this.owner = owner;
            this.member = member;
        }

        public boolean isClassSearch() {
            return member == null;
        }

        public boolean matchesClass(String internalName) {
            if (owner == null) return false;
            String norm = internalName.replace('.', '/').replaceAll("^/+", "");
            return norm.equals(owner) || norm.endsWith("/" + owner);
        }

        public boolean matchesMethod(String methodOwner, String methodName) {
            if (member == null) return false;
            if (!member.equals(methodName)) return false;
            if (owner == null) return true;
            return matchesClass(methodOwner);
        }

        public boolean matchesField(String fieldOwner, String fieldName) {
            return matchesMethod(fieldOwner, fieldName);
        }

        public static QueryTarget parse(String rawQuery) {
            String q = rawQuery.trim().replace('\\', '/').replaceAll("^/+", "");
            if (q.endsWith(".class")) {
                q = q.substring(0, q.length() - 6);
            } else if (q.endsWith(".java")) {
                q = q.substring(0, q.length() - 5);
            }

            // 1. Format: ClassName#member or #member
            if (q.contains("#")) {
                int hashIdx = q.indexOf('#');
                String rawOwner = hashIdx > 0 ? q.substring(0, hashIdx).trim() : null;
                String rawMember = q.substring(hashIdx + 1).trim();
                // strip optional signature e.g. myMethod(String) -> myMethod
                if (rawMember.contains("(")) {
                    rawMember = rawMember.substring(0, rawMember.indexOf('(')).trim();
                }
                String normOwner = rawOwner != null && !rawOwner.isEmpty() ? rawOwner.replace('.', '/') : null;
                return new QueryTarget(normOwner, rawMember);
            }

            // 2. Format: ClassName.member (where last token starts with lowercase, e.g. com.example.Service.doWork)
            int lastDot = q.lastIndexOf('.');
            int lastSlash = q.lastIndexOf('/');
            int lastSep = Math.max(lastDot, lastSlash);
            if (lastSep > 0 && lastSep < q.length() - 1) {
                String potentialMember = q.substring(lastSep + 1);
                // If the token starts with lowercase and previous token starts with uppercase (Class.method convention)
                if (Character.isLowerCase(potentialMember.charAt(0)) && !potentialMember.contains("/")) {
                    int prevSep = Math.max(q.lastIndexOf('.', lastSep - 1), q.lastIndexOf('/', lastSep - 1));
                    String className = prevSep >= 0 ? q.substring(prevSep + 1, lastSep) : q.substring(0, lastSep);
                    if (!className.isEmpty() && Character.isUpperCase(className.charAt(0))) {
                        String normOwner = q.substring(0, lastSep).replace('.', '/');
                        return new QueryTarget(normOwner, potentialMember);
                    }
                }
            }

            // 3. Class search
            return new QueryTarget(q.replace('.', '/'), null);
        }
    }
}
