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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Ultra-fast class metadata and skeleton extractor using ASM without decompiling bytecode.
 * Extracts authentic method parameter names when available from debug symbols or -parameters flag.
 */
public class AsmClassOutlineExtractor {

    public static ClassOutline extract(byte[] bytecode) {
        // Ensure compatibility with newer bytecode versions (e.g. Java 22-25) by patching version bytes for ASM reader
        byte[] compatibleBytecode = bytecode;
        if (bytecode.length > 7) {
            int majorVersion = ((bytecode[6] & 0xFF) << 8) | (bytecode[7] & 0xFF);
            if (majorVersion > 65) { // ASM 9.7 officially supports up to Java 21 (major 65)
                compatibleBytecode = bytecode.clone();
                compatibleBytecode[6] = 0;
                compatibleBytecode[7] = 65; // Treat as Java 21 for ASM AST reading
            }
        }

        ClassReader reader = new ClassReader(compatibleBytecode);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);

        String internalName = classNode.name;
        String packageName = "";
        int lastSlash = internalName.lastIndexOf('/');
        if (lastSlash >= 0) {
            packageName = internalName.substring(0, lastSlash).replace('/', '.');
        }

        String classKind = "class";
        if ((classNode.access & Opcodes.ACC_INTERFACE) != 0) {
            classKind = (classNode.access & Opcodes.ACC_ANNOTATION) != 0 ? "@interface" : "interface";
        } else if ((classNode.access & Opcodes.ACC_ENUM) != 0) {
            classKind = "enum";
        } else if ((classNode.access & Opcodes.ACC_RECORD) != 0) {
            classKind = "record";
        }

        String accessModifiers = parseAccessModifiers(classNode.access);
        List<String> annotations = extractAnnotations(classNode.visibleAnnotations);

        List<ClassOutline.FieldOutline> fields = new ArrayList<>();
        if (classNode.fields != null) {
            for (FieldNode fn : classNode.fields) {
                String fieldType = Type.getType(fn.desc).getClassName();
                fields.add(new ClassOutline.FieldOutline(
                        fn.name,
                        fieldType,
                        parseAccessModifiers(fn.access),
                        extractAnnotations(fn.visibleAnnotations)
                ));
            }
        }

        List<ClassOutline.MethodOutline> methods = new ArrayList<>();
        if (classNode.methods != null) {
            for (MethodNode mn : classNode.methods) {
                Type methodType = Type.getMethodType(mn.desc);
                String returnType = methodType.getReturnType().getClassName();
                Type[] argTypes = methodType.getArgumentTypes();

                List<ClassOutline.ParameterOutline> parameters = new ArrayList<>();
                boolean isStatic = (mn.access & Opcodes.ACC_STATIC) != 0;
                int varOffset = isStatic ? 0 : 1;

                for (int i = 0; i < argTypes.length; i++) {
                    String typeName = argTypes[i].getClassName();
                    String paramName = null;

                    // 1. Check -parameters flag info
                    if (mn.parameters != null && i < mn.parameters.size()) {
                        ParameterNode pn = mn.parameters.get(i);
                        if (pn != null && pn.name != null && !pn.name.isBlank()) {
                            paramName = pn.name;
                        }
                    }

                    // 2. Fall back to local variable table
                    if (paramName == null && mn.localVariables != null) {
                        for (LocalVariableNode lvn : mn.localVariables) {
                            if (lvn.index == (varOffset + i) && lvn.name != null && !lvn.name.equals("this")) {
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
                        parseAccessModifiers(mn.access),
                        extractAnnotations(mn.visibleAnnotations)
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
                fields,
                methods,
                classNode.version & 0xFFFF
        );
    }

    private static String parseAccessModifiers(int access) {
        List<String> mods = new ArrayList<>();
        if ((access & Opcodes.ACC_PUBLIC) != 0) mods.add("public");
        if ((access & Opcodes.ACC_PROTECTED) != 0) mods.add("protected");
        if ((access & Opcodes.ACC_PRIVATE) != 0) mods.add("private");
        if ((access & Opcodes.ACC_STATIC) != 0) mods.add("static");
        if ((access & Opcodes.ACC_FINAL) != 0) mods.add("final");
        if ((access & Opcodes.ACC_ABSTRACT) != 0 && (access & Opcodes.ACC_INTERFACE) == 0) mods.add("abstract");
        if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) mods.add("synchronized");
        return String.join(" ", mods);
    }

    private static List<String> extractAnnotations(List<AnnotationNode> annotations) {
        if (annotations == null) return Collections.emptyList();
        List<String> list = new ArrayList<>();
        for (AnnotationNode an : annotations) {
            list.add(Type.getType(an.desc).getClassName());
        }
        return list;
    }
}
