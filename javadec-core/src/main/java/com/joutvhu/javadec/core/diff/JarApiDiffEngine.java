package com.joutvhu.javadec.core.diff;

import com.joutvhu.javadec.core.model.ClassOutline;
import com.joutvhu.javadec.core.model.JarApiDiffResult;
import com.joutvhu.javadec.core.model.JarApiDiffResult.DiffItem;
import com.joutvhu.javadec.core.model.JarApiDiffResult.DiffType;
import com.joutvhu.javadec.core.outline.AsmClassOutlineExtractor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * High-performance binary API delta & breaking changes comparator between two JAR archives.
 * Analyzes public/protected API contracts via ASM to pinpoint added, removed, and modified methods/classes/fields.
 */
public class JarApiDiffEngine {

    private static final String[] ARCHIVE_CLASS_PREFIXES = {"", "BOOT-INF/classes/", "WEB-INF/classes/"};

    public static JarApiDiffResult compare(File jar1, File jar2, String packageFilter, boolean breakingOnly) throws IOException {
        Map<String, ClassOutline> api1 = extractPublicApi(jar1, packageFilter);
        Map<String, ClassOutline> api2 = extractPublicApi(jar2, packageFilter);

        List<DiffItem> diffs = new ArrayList<>();
        int added = 0;
        int removed = 0;
        int modified = 0;

        // 1. Check removed or modified classes
        for (Map.Entry<String, ClassOutline> entry : api1.entrySet()) {
            String className = entry.getKey();
            ClassOutline o1 = entry.getValue();
            ClassOutline o2 = api2.get(className);

            if (o2 == null) {
                diffs.add(new DiffItem(DiffType.CLASS_REMOVED, className, "Class deleted or made non-public", true));
                removed++;
            } else {
                // Compare methods
                Map<String, String> m1Map = methodMap(o1);
                Map<String, String> m2Map = methodMap(o2);

                for (Map.Entry<String, String> mEntry : m1Map.entrySet()) {
                    String mKey = mEntry.getKey();
                    if (!m2Map.containsKey(mKey)) {
                        diffs.add(new DiffItem(DiffType.METHOD_REMOVED, className + "." + mKey, "Method removed", true));
                        removed++;
                    } else if (!Objects.equals(mEntry.getValue(), m2Map.get(mKey))) {
                        diffs.add(new DiffItem(DiffType.METHOD_MODIFIED, className + "." + mKey,
                                "Changed return type or modifiers: " + mEntry.getValue() + " -> " + m2Map.get(mKey), true));
                        modified++;
                    }
                }

                if (!breakingOnly) {
                    for (Map.Entry<String, String> mEntry : m2Map.entrySet()) {
                        String mKey = mEntry.getKey();
                        if (!m1Map.containsKey(mKey)) {
                            diffs.add(new DiffItem(DiffType.METHOD_ADDED, className + "." + mKey, "New method added", false));
                            added++;
                        }
                    }
                }

                // Compare fields
                Map<String, String> f1Map = fieldMap(o1);
                Map<String, String> f2Map = fieldMap(o2);

                for (Map.Entry<String, String> fEntry : f1Map.entrySet()) {
                    String fKey = fEntry.getKey();
                    if (!f2Map.containsKey(fKey)) {
                        diffs.add(new DiffItem(DiffType.FIELD_REMOVED, className + "." + fKey, "Field removed", true));
                        removed++;
                    }
                }

                if (!breakingOnly) {
                    for (Map.Entry<String, String> fEntry : f2Map.entrySet()) {
                        String fKey = fEntry.getKey();
                        if (!f1Map.containsKey(fKey)) {
                            diffs.add(new DiffItem(DiffType.FIELD_ADDED, className + "." + fKey, "New field added", false));
                            added++;
                        }
                    }
                }
            }
        }

        // 2. Check added classes
        if (!breakingOnly) {
            for (String className : api2.keySet()) {
                if (!api1.containsKey(className)) {
                    diffs.add(new DiffItem(DiffType.CLASS_ADDED, className, "New public class added", false));
                    added++;
                }
            }
        }

        StringBuilder report = new StringBuilder();
        report.append("API Binary Diff: ").append(jar1.getName()).append(" -> ").append(jar2.getName()).append("\n");
        if (packageFilter != null && !packageFilter.isBlank()) {
            report.append("Filter: ").append(packageFilter).append("\n");
        }
        report.append(String.format("Summary: %d added, %d removed, %d modified (Total: %d)\n\n", added, removed, modified, diffs.size()));

        if (diffs.isEmpty()) {
            report.append("// No public API differences detected.\n");
        } else {
            for (DiffItem diff : diffs) {
                report.append(diff.toDisplayString()).append("\n");
            }
        }

        return new JarApiDiffResult(jar1.getName(), jar2.getName(), added, removed, modified, diffs, report.toString());
    }

    private static Map<String, String> methodMap(ClassOutline outline) {
        Map<String, String> map = new LinkedHashMap<>();
        if (outline.methods() != null) {
            for (ClassOutline.MethodOutline m : outline.methods()) {
                if (m.name().equals("<clinit>")) continue;
                String access = m.accessModifiers();
                if (access.contains("private")) continue;

                StringBuilder sig = new StringBuilder(m.name()).append("(");
                if (m.parameters() != null) {
                    for (int i = 0; i < m.parameters().size(); i++) {
                        sig.append(m.parameters().get(i).type());
                        if (i < m.parameters().size() - 1) sig.append(", ");
                    }
                }
                sig.append(")");
                map.put(sig.toString(), access + " " + m.returnType());
            }
        }
        return map;
    }

    private static Map<String, String> fieldMap(ClassOutline outline) {
        Map<String, String> map = new LinkedHashMap<>();
        if (outline.fields() != null) {
            for (ClassOutline.FieldOutline f : outline.fields()) {
                String access = f.accessModifiers();
                if (access.contains("private")) continue;
                map.put(f.name(), access + " " + f.descriptor());
            }
        }
        return map;
    }

    private static Map<String, ClassOutline> extractPublicApi(File jar, String packageFilter) throws IOException {
        Map<String, ClassOutline> api = new TreeMap<>();
        String cleanFilter = packageFilter != null ? packageFilter.replace('\\', '/').replaceAll("^/+", "").replace('.', '/') : null;

        try (ZipFile zip = new ZipFile(jar)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
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

                if (cleanFilter != null && !cleanName.startsWith(cleanFilter)) {
                    continue;
                }

                try (InputStream is = zip.getInputStream(entry)) {
                    byte[] bytes = is.readAllBytes();
                    ClassOutline outline = AsmClassOutlineExtractor.extract(bytes);
                    if (outline.accessModifiers().contains("public") || outline.accessModifiers().contains("protected")) {
                        api.put(outline.className().replace('/', '.'), outline);
                    }
                } catch (Exception ignored) {}
            }
        }
        return api;
    }
}
