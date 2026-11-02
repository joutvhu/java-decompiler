package com.joutvhu.javadec.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the outline (skeleton/signature) of a Java class/interface/record/enum/module.
 * Extremely token-efficient, allowing AI agents to understand API structures without full method bodies.
 * Natively renders clean, syntactically valid Java syntax for classes, interfaces, records, enums, and JPMS modules.
 */
public record ClassOutline(
        String className,
        String packageName,
        String accessModifiers,
        String classKind, // class, interface, enum, record, @interface, module, open module
        String superClassName,
        List<String> interfaces,
        List<String> annotations,
        List<RecordComponentOutline> recordComponents,
        List<String> enumConstants,
        List<FieldOutline> fields,
        List<MethodOutline> methods,
        List<String> moduleDirectives,
        int bytecodeMajorVersion
) {
    public ClassOutline(
            String className,
            String packageName,
            String accessModifiers,
            String classKind,
            String superClassName,
            List<String> interfaces,
            List<String> annotations,
            List<RecordComponentOutline> recordComponents,
            List<String> enumConstants,
            List<FieldOutline> fields,
            List<MethodOutline> methods,
            int bytecodeMajorVersion
    ) {
        this(className, packageName, accessModifiers, classKind, superClassName, interfaces, annotations,
                recordComponents, enumConstants, fields, methods, List.of(), bytecodeMajorVersion);
    }

    public record RecordComponentOutline(
            String type,
            String name
    ) {}

    public record FieldOutline(
            String name,
            String descriptor,
            String accessModifiers,
            List<String> annotations
    ) {}

    public record ParameterOutline(
            String type,
            String name
    ) {}

    public record MethodOutline(
            String name,
            String returnType,
            List<ParameterOutline> parameters,
            List<String> exceptions,
            String accessModifiers,
            List<String> annotations
    ) {}

    /**
     * Formats this outline into a clean Java skeleton source string for AI consumption.
     */
    public String toSkeletonSource() {
        StringBuilder sb = new StringBuilder();

        // Render JPMS module-info e.g. module org.example { exports ...; requires ...; }
        if ("module".equals(classKind) || "open module".equals(classKind)) {
            for (String ann : annotations) {
                sb.append("@").append(ann).append("\n");
            }
            sb.append(classKind).append(" ").append(className).append(" {\n");
            if (moduleDirectives != null) {
                for (String dir : moduleDirectives) {
                    sb.append("    ").append(dir).append(";\n");
                }
            }
            sb.append("}\n");
            return sb.toString();
        }

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

        boolean isInterface = "interface".equals(classKind) || "@interface".equals(classKind);
        boolean isRecord = "record".equals(classKind);
        boolean isEnum = "enum".equals(classKind);

        // Render record components e.g. record User(String name, int age) or record EmptyRecord()
        if (isRecord) {
            sb.append("(");
            if (recordComponents != null) {
                for (int i = 0; i < recordComponents.size(); i++) {
                    RecordComponentOutline rc = recordComponents.get(i);
                    sb.append(rc.type()).append(" ").append(rc.name());
                    if (i < recordComponents.size() - 1) {
                        sb.append(", ");
                    }
                }
            }
            sb.append(")");
        }

        // Extends clause (classes only; records implicitly extend Record, enums implicitly extend Enum)
        if (!isInterface && !isRecord && !isEnum
                && superClassName != null
                && !superClassName.equals("java/lang/Object")
                && !superClassName.equals("java/lang/Record")
                && !superClassName.equals("java/lang/Enum")
                && !superClassName.isEmpty()) {
            sb.append(" extends ").append(simpleName(superClassName));
        }

        // Interfaces (for interfaces use 'extends', for classes/records/enums use 'implements')
        if (interfaces != null && !interfaces.isEmpty()) {
            if (isInterface) {
                sb.append(" extends ").append(String.join(", ", interfaces.stream().map(ClassOutline::simpleName).toList()));
            } else {
                sb.append(" implements ").append(String.join(", ", interfaces.stream().map(ClassOutline::simpleName).toList()));
            }
        }

        sb.append(" {\n");

        // Render enum constants e.g. ACTIVE, INACTIVE;
        if (isEnum && enumConstants != null && !enumConstants.isEmpty()) {
            sb.append("    ");
            sb.append(String.join(", ", enumConstants));
            sb.append(";\n\n");
        }

        if (fields != null && !fields.isEmpty()) {
            boolean hasHeader = false;
            for (FieldOutline field : fields) {
                if (!hasHeader) {
                    sb.append("    // Fields\n");
                    hasHeader = true;
                }
                for (String ann : field.annotations()) {
                    sb.append("    @").append(ann).append("\n");
                }
                sb.append("    ");
                if (!field.accessModifiers().isEmpty()) {
                    sb.append(field.accessModifiers()).append(" ");
                }
                sb.append(field.descriptor()).append(" ").append(field.name()).append(";\n");
            }
            if (hasHeader) {
                sb.append("\n");
            }
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

                if (method.parameters() != null) {
                    for (int i = 0; i < method.parameters().size(); i++) {
                        ParameterOutline p = method.parameters().get(i);
                        sb.append(p.type()).append(" ").append(p.name());
                        if (i < method.parameters().size() - 1) {
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
        int lastSeparator = Math.max(internalName.lastIndexOf('/'), internalName.lastIndexOf('.'));
        return lastSeparator >= 0 ? internalName.substring(lastSeparator + 1) : internalName;
    }
}
