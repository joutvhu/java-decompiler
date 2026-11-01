package com.joutvhu.javadec.core.outline;

import com.joutvhu.javadec.core.model.ClassOutline;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;
import org.objectweb.asm.tree.RecordComponentNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ultra-fast class metadata and skeleton extractor using ASM without decompiling bytecode.
 * Extracts authentic method parameter names when available from debug symbols or -parameters flag,
 * accurately handling 2-slot types (long/double) in the local variable table,
 * and capturing both RUNTIME and CLASS retention annotations.
 */
public class AsmClassOutlineExtractor {

    public static ClassOutline extract(byte[] bytecode) {
        // Ensure compatibility with newer bytecode versions (e.g. Java 22-27+) by patching version bytes for ASM reader
        byte[] compatibleBytecode = bytecode;
        int originalMajorVersion = (bytecode != null && bytecode.length > 7)
                ? (((bytecode[6] & 0xFF) << 8) | (bytecode[7] & 0xFF))
                : 0;
        if (bytecode != null && bytecode.length > 7) {
            if (originalMajorVersion > 65) { // ASM 9.7 officially supports up to Java 21 (major 65)
                compatibleBytecode = bytecode.clone();
                compatibleBytecode[6] = 0;
                compatibleBytecode[7] = 65; // Treat as Java 21 for ASM AST reading
            }
        }

        ClassReader reader = new ClassReader(compatibleBytecode);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.SKIP_FRAMES);

        String internalName = classNode.name;
        String packageName = "";
        int lastSlash = internalName.lastIndexOf('/');
        if (lastSlash >= 0) {
            packageName = internalName.substring(0, lastSlash).replace('/', '.');
        }

        boolean isInterface = (classNode.access & Opcodes.ACC_INTERFACE) != 0;
        String classKind = "class";
        if (isInterface) {
            classKind = (classNode.access & Opcodes.ACC_ANNOTATION) != 0 ? "@interface" : "interface";
        } else if ((classNode.access & Opcodes.ACC_ENUM) != 0) {
            classKind = "enum";
        } else if ((classNode.access & Opcodes.ACC_RECORD) != 0) {
            classKind = "record";
        }

        String accessModifiers = parseClassModifiers(classNode.access);
        List<String> annotations = extractAnnotations(classNode.visibleAnnotations, classNode.invisibleAnnotations);

        // Record components e.g. User(String name, int age)
        List<ClassOutline.RecordComponentOutline> recordComponents = new ArrayList<>();
        if (classNode.recordComponents != null) {
            for (RecordComponentNode rcn : classNode.recordComponents) {
                String compType = Type.getType(rcn.descriptor).getClassName();
                recordComponents.add(new ClassOutline.RecordComponentOutline(compType, rcn.name));
            }
        }

        // Enum constants and regular fields
        List<String> enumConstants = new ArrayList<>();
        List<ClassOutline.FieldOutline> fields = new ArrayList<>();
        if (classNode.fields != null) {
            for (FieldNode fn : classNode.fields) {
                if ((fn.access & Opcodes.ACC_SYNTHETIC) != 0) {
                    continue; // Skip synthetic fields e.g. $VALUES
                }

                if ((fn.access & Opcodes.ACC_ENUM) != 0) {
                    enumConstants.add(fn.name);
                } else {
                    String fieldType = Type.getType(fn.desc).getClassName();
                    fields.add(new ClassOutline.FieldOutline(
                            fn.name,
                            fieldType,
                            parseFieldModifiers(fn.access),
                            extractAnnotations(fn.visibleAnnotations, fn.invisibleAnnotations)
                    ));
                }
            }
        }

        List<ClassOutline.MethodOutline> methods = new ArrayList<>();
        if (classNode.methods != null) {
            for (MethodNode mn : classNode.methods) {
                if ((mn.access & Opcodes.ACC_SYNTHETIC) != 0) {
                    continue; // Skip compiler synthetic bridge methods
                }

                Type methodType = Type.getMethodType(mn.desc);
                String returnType = methodType.getReturnType().getClassName();
                Type[] argTypes = methodType.getArgumentTypes();

                List<ClassOutline.ParameterOutline> parameters = new ArrayList<>();
                boolean isStatic = (mn.access & Opcodes.ACC_STATIC) != 0;
                int currentSlot = isStatic ? 0 : 1;

                for (int i = 0; i < argTypes.length; i++) {
                    Type argType = argTypes[i];
                    String typeName = argType.getClassName();
                    String paramName = null;
                    int targetSlot = currentSlot;
                    currentSlot += argType.getSize(); // 2 slots for long and double, 1 slot for others

                    // 1. Check -parameters flag info
                    if (mn.parameters != null && i < mn.parameters.size()) {
                        ParameterNode pn = mn.parameters.get(i);
                        if (pn != null && pn.name != null && !pn.name.isBlank()) {
                            paramName = pn.name;
                        }
                    }

                    // 2. Fall back to local variable table matching targetSlot
                    if (paramName == null && mn.localVariables != null) {
                        for (LocalVariableNode lvn : mn.localVariables) {
                            if (lvn.index == targetSlot && lvn.name != null && !lvn.name.equals("this")) {
                                paramName = lvn.name;
                                break;
                            }
                        }
                    }

                    // 3. Fall back to generic arg<i>
                    if (paramName == null) {
                        paramName = "arg" + i;
                    }

                    parameters.add(new ClassOutline.ParameterOutline(typeName, paramName));
                }

                List<String> exceptions = mn.exceptions != null ? mn.exceptions : Collections.emptyList();

                methods.add(new ClassOutline.MethodOutline(
                        mn.name,
                        returnType,
                        parameters,
                        exceptions,
                        parseMethodModifiers(mn.access, isInterface),
                        extractAnnotations(mn.visibleAnnotations, mn.invisibleAnnotations)
                ));
            }
        }

        List<String> interfaces = classNode.interfaces != null ? classNode.interfaces : Collections.emptyList();

        return new ClassOutline(
                internalName,
                packageName,
                accessModifiers,
                classKind,
                classNode.superName,
                interfaces,
                annotations,
                recordComponents,
                enumConstants,
                fields,
                methods,
                originalMajorVersion > 0 ? originalMajorVersion : (classNode.version & 0xFFFF)
        );
    }

    private static String parseClassModifiers(int access) {
        List<String> mods = new ArrayList<>();
        if ((access & Opcodes.ACC_PUBLIC) != 0) mods.add("public");
        if ((access & Opcodes.ACC_PROTECTED) != 0) mods.add("protected");
        if ((access & Opcodes.ACC_PRIVATE) != 0) mods.add("private");
        if ((access & Opcodes.ACC_STATIC) != 0) mods.add("static");
        if ((access & Opcodes.ACC_FINAL) != 0) mods.add("final");
        if ((access & Opcodes.ACC_ABSTRACT) != 0 && (access & Opcodes.ACC_INTERFACE) == 0) mods.add("abstract");
        return String.join(" ", mods);
    }

    private static String parseFieldModifiers(int access) {
        List<String> mods = new ArrayList<>();
        if ((access & Opcodes.ACC_PUBLIC) != 0) mods.add("public");
        if ((access & Opcodes.ACC_PROTECTED) != 0) mods.add("protected");
        if ((access & Opcodes.ACC_PRIVATE) != 0) mods.add("private");
        if ((access & Opcodes.ACC_STATIC) != 0) mods.add("static");
        if ((access & Opcodes.ACC_FINAL) != 0) mods.add("final");
        if ((access & Opcodes.ACC_VOLATILE) != 0) mods.add("volatile");
        if ((access & Opcodes.ACC_TRANSIENT) != 0) mods.add("transient");
        return String.join(" ", mods);
    }

    private static String parseMethodModifiers(int access, boolean isInterface) {
        List<String> mods = new ArrayList<>();
        if ((access & Opcodes.ACC_PUBLIC) != 0) mods.add("public");
        if ((access & Opcodes.ACC_PROTECTED) != 0) mods.add("protected");
        if ((access & Opcodes.ACC_PRIVATE) != 0) mods.add("private");
        if ((access & Opcodes.ACC_STATIC) != 0) mods.add("static");
        if ((access & Opcodes.ACC_FINAL) != 0) mods.add("final");
        if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) mods.add("synchronized");
        if ((access & Opcodes.ACC_NATIVE) != 0) mods.add("native");
        if ((access & Opcodes.ACC_ABSTRACT) != 0) {
            if (!isInterface) {
                mods.add("abstract");
            }
        } else if (isInterface && (access & Opcodes.ACC_STATIC) == 0 && (access & Opcodes.ACC_PRIVATE) == 0) {
            mods.add("default");
        }
        return String.join(" ", mods);
    }

    private static List<String> extractAnnotations(List<AnnotationNode> visible, List<AnnotationNode> invisible) {
        if (visible == null && invisible == null) return Collections.emptyList();
        List<String> list = new ArrayList<>();
        if (visible != null) {
            for (AnnotationNode an : visible) {
                list.add(Type.getType(an.desc).getClassName());
            }
        }
        if (invisible != null) {
            for (AnnotationNode an : invisible) {
                String name = Type.getType(an.desc).getClassName();
                if (!list.contains(name)) {
                    list.add(name);
                }
            }
        }
        return list;
    }
}
