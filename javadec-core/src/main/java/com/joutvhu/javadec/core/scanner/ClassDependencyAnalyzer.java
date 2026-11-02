package com.joutvhu.javadec.core.scanner;

import com.joutvhu.javadec.core.model.DependencyAnalysisResult;
import org.objectweb.asm.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Analyzes compiled class bytecode to extract and categorize all imported classes and dependencies.
 */
public class ClassDependencyAnalyzer {

    public static DependencyAnalysisResult analyze(byte[] bytecode) {
        if (bytecode == null || bytecode.length < 8) {
            return DependencyAnalysisResult.of("unknown", Set.of(), Set.of(), Set.of());
        }

        byte[] compatible = bytecode;
        int majorVersion = ((bytecode[6] & 0xFF) << 8) | (bytecode[7] & 0xFF);
        if (majorVersion > 65) {
            compatible = bytecode.clone();
            compatible[6] = 0;
            compatible[7] = 65;
        }

        final String[] classHolder = new String[]{"unknown"};
        final Set<String> rawTypes = new HashSet<>();

        ClassReader reader = new ClassReader(compatible);
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                classHolder[0] = name;
                if (superName != null) addInternalType(superName, rawTypes);
                if (interfaces != null) {
                    for (String iface : interfaces) addInternalType(iface, rawTypes);
                }
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                addDescriptorTypes(descriptor, rawTypes);
                return null;
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                addDescriptorTypes(descriptor, rawTypes);
                return new FieldVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String desc, boolean vis) {
                        addDescriptorTypes(desc, rawTypes);
                        return null;
                    }
                };
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                addDescriptorTypes(descriptor, rawTypes);
                if (exceptions != null) {
                    for (String exc : exceptions) addInternalType(exc, rawTypes);
                }

                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String desc, boolean vis) {
                        addDescriptorTypes(desc, rawTypes);
                        return null;
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        addInternalType(owner, rawTypes);
                        addDescriptorTypes(descriptor, rawTypes);
                    }

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                        addInternalType(owner, rawTypes);
                        addDescriptorTypes(descriptor, rawTypes);
                    }

                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        addInternalType(type, rawTypes);
                    }

                    @Override
                    public void visitLdcInsn(Object value) {
                        if (value instanceof Type t) {
                            addInternalType(t.getInternalName(), rawTypes);
                        }
                    }
                };
            }
        }, ClassReader.SKIP_FRAMES);

        String mainClass = classHolder[0];
        String basePkg = getBasePackage(mainClass);

        Set<String> jdk = new HashSet<>();
        Set<String> internal = new HashSet<>();
        Set<String> thirdParty = new HashSet<>();

        for (String type : rawTypes) {
            String dotted = type.replace('/', '.');
            if (dotted.equals(mainClass.replace('/', '.')) || dotted.equals("void") || dotted.startsWith("[")) {
                continue;
            }

            if (dotted.startsWith("java.") || dotted.startsWith("javax.") || dotted.startsWith("jdk.")
                    || dotted.startsWith("org.w3c.dom.") || dotted.startsWith("org.xml.sax.")) {
                jdk.add(dotted);
            } else if (!basePkg.isEmpty() && (dotted.startsWith(basePkg + ".") || dotted.equals(basePkg))) {
                internal.add(dotted);
            } else {
                thirdParty.add(dotted);
            }
        }

        return DependencyAnalysisResult.of(mainClass, jdk, internal, thirdParty);
    }

    private static void addInternalType(String internalName, Set<String> types) {
        if (internalName != null && !internalName.startsWith("[")) {
            types.add(internalName);
        }
    }

    private static void addDescriptorTypes(String descriptor, Set<String> types) {
        if (descriptor == null) return;
        int i = 0;
        while (i < descriptor.length()) {
            if (descriptor.charAt(i) == 'L') {
                int semi = descriptor.indexOf(';', i);
                if (semi > i) {
                    types.add(descriptor.substring(i + 1, semi));
                    i = semi + 1;
                    continue;
                }
            }
            i++;
        }
    }

    private static String getBasePackage(String internalName) {
        int firstSlash = internalName.indexOf('/');
        if (firstSlash < 0) return "";
        int secondSlash = internalName.indexOf('/', firstSlash + 1);
        if (secondSlash < 0) return internalName.substring(0, firstSlash).replace('/', '.');
        return internalName.substring(0, secondSlash).replace('/', '.');
    }
}
