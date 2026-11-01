package com.joutvhu.javadec.core.model;

import java.util.List;

/**
 * Represents the outline (skeleton/signature) of a Java class/interface/record/enum.
 * Extremely token-efficient, allowing AI agents to understand API structures without full method bodies.
 */
public record ClassOutline(
        String className,
        String packageName,
        String accessModifiers,
        String classKind, // class, interface, enum, record, @interface
        String superClassName,
        List<String> interfaces,
        List<String> annotations,
        List<FieldOutline> fields,
        List<MethodOutline> methods,
        int bytecodeMajorVersion
) {
    public record FieldOutline(
            String name,
            String descriptor,
            String accessModifiers,
            List<String> annotations
    ) {}

    public record MethodOutline(
            String name,
            String returnType,
            List<String> parameterTypes,
            List<String> exceptions,
            String accessModifiers,
            List<String> annotations
    ) {}

    /**
     * Formats this outline into a clean Java skeleton source string for AI consumption.
     */
    public String toSkeletonSource() {
        StringBuilder sb = new StringBuilder();
        if (packageName != null && !packageName.isEmpty()) {
            sb.append("package ").append(packageName).append(";\n\n");
        }

        for (String ann : annotations) {
            sb.append("@").append(ann).append("\n");
        }

        if (!accessModifiers.isEmpty()) {
            sb.append(accessModifiers).append(" ");
        }
        sb.append(classKind).append(" ").append(simpleName(className));

        if (superClassName != null && !superClassName.equals("java/lang/Object") && !superClassName.isEmpty()) {
            sb.append(" extends ").append(simpleName(superClassName));
        }

        if (interfaces != null && !interfaces.isEmpty()) {
            sb.append(" implements ").append(String.join(", ", interfaces.stream().map(ClassOutline::simpleName).toList()));
        }

        sb.append(" {\n");

        if (fields != null && !fields.isEmpty()) {
            sb.append("    // Fields\n");
            for (FieldOutline field : fields) {
                for (String ann : field.annotations()) {
                    sb.append("    @").append(ann).append("\n");
                }
                sb.append("    ");
                if (!field.accessModifiers().isEmpty()) {
                    sb.append(field.accessModifiers()).append(" ");
                }
                sb.append(field.descriptor()).append(" ").append(field.name()).append(";\n");
            }
            sb.append("\n");
        }

        if (methods != null && !methods.isEmpty()) {
            sb.append("    // Methods\n");
            for (MethodOutline method : methods) {
                if ("<clinit>".equals(method.name())) {
                    continue; // Skip static initializer block in outline
                }

                for (String ann : method.annotations()) {
                    sb.append("    @").append(ann).append("\n");
                }
                sb.append("    ");
                if (!method.accessModifiers().isEmpty()) {
                    sb.append(method.accessModifiers()).append(" ");
                }

                boolean isConstructor = "<init>".equals(method.name());
                if (isConstructor) {
                    sb.append(simpleName(className)).append("(");
                } else {
                    sb.append(method.returnType()).append(" ").append(method.name()).append("(");
                }

                if (method.parameterTypes() != null) {
                    for (int i = 0; i < method.parameterTypes().size(); i++) {
                        sb.append(method.parameterTypes().get(i)).append(" arg").append(i);
                        if (i < method.parameterTypes().size() - 1) {
                            sb.append(", ");
                        }
                    }
                }
                sb.append(")");
                if (method.exceptions() != null && !method.exceptions().isEmpty()) {
                    sb.append(" throws ").append(String.join(", ", method.exceptions().stream().map(ClassOutline::simpleName).toList()));
                }
                sb.append(";\n");
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    private static String simpleName(String internalName) {
        if (internalName == null) return "";
        int lastSlash = internalName.lastIndexOf('/');
        return lastSlash >= 0 ? internalName.substring(lastSlash + 1) : internalName;
    }
}
