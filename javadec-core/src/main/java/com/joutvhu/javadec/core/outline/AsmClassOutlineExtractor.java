package com.joutvhu.javadec.core.outline;

import com.joutvhu.javadec.core.model.ClassOutline;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Ultra-fast class metadata and skeleton extractor using ASM without decompiling bytecode.
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
        reader.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

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
                List<String> paramTypes = Arrays.stream(methodType.getArgumentTypes())
                        .map(Type::getClassName)
                        .toList();

                List<String> exceptions = mn.exceptions != null ? mn.exceptions : Collections.emptyList();

                methods.add(new ClassOutline.MethodOutline(
                        mn.name,
                        returnType,
                        paramTypes,
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
