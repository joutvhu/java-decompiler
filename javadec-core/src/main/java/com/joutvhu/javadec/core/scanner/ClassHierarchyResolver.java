package com.joutvhu.javadec.core.scanner;

import com.joutvhu.javadec.core.loader.AuxiliaryClasspathResolver;
import com.joutvhu.javadec.core.loader.ZipBytecodeLoader;
import com.joutvhu.javadec.core.model.ClassHierarchyResult;
import com.joutvhu.javadec.core.model.ClassOutline;
import com.joutvhu.javadec.core.outline.AsmClassOutlineExtractor;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Traverses and resolves the complete inheritance chain (superclasses up to Object)
 * and all direct/indirect implemented interfaces across an archive and auxiliary classpath.
 */
public class ClassHierarchyResolver {

    public static ClassHierarchyResult resolve(File archiveFile, byte[] rootBytecode, String rootClassName) {
        ClassOutline rootOutline;
        if (rootBytecode != null) {
            rootOutline = AsmClassOutlineExtractor.extract(rootBytecode);
        } else if (archiveFile != null && rootClassName != null) {
            try {
                byte[] bytes = ZipBytecodeLoader.loadClassBytes(archiveFile, rootClassName);
                rootOutline = AsmClassOutlineExtractor.extract(bytes);
            } catch (Exception e) {
                return new ClassHierarchyResult(rootClassName, List.of(), Map.of(), "// Failed to load class: " + rootClassName);
            }
        } else {
            return new ClassHierarchyResult("unknown", List.of(), Map.of(), "// Invalid class specification.");
        }

        List<File> auxJars = archiveFile != null ? AuxiliaryClasspathResolver.resolve(archiveFile, null, true) : List.of();

        List<String> superChain = new ArrayList<>();
        Map<String, String> allInterfaces = new LinkedHashMap<>(); // iface -> viaClass
        Set<String> visitedClasses = new HashSet<>();

        String currentClass = rootOutline.className().replace('/', '.');
        superChain.add(currentClass);
        visitedClasses.add(rootOutline.className().replace('.', '/'));

        // Record root interfaces
        if (rootOutline.interfaces() != null) {
            for (String iface : rootOutline.interfaces()) {
                allInterfaces.put(iface.replace('/', '.'), currentClass);
            }
        }

        String nextSuper = rootOutline.superClassName();
        while (nextSuper != null && !nextSuper.isEmpty() && visitedClasses.add(nextSuper.replace('.', '/'))) {
            String dottedSuper = nextSuper.replace('/', '.');
            superChain.add(dottedSuper);
            if ("java.lang.Object".equals(dottedSuper)) break;

            ClassOutline superOutline = findOutline(nextSuper, archiveFile, auxJars);
            if (superOutline != null) {
                if (superOutline.interfaces() != null) {
                    for (String iface : superOutline.interfaces()) {
                        allInterfaces.putIfAbsent(iface.replace('/', '.'), dottedSuper);
                    }
                }
                nextSuper = superOutline.superClassName();
            } else {
                break;
            }
        }

        // Recursively resolve super-interfaces of all collected interfaces
        Queue<String> ifaceQueue = new ArrayDeque<>(allInterfaces.keySet());
        Set<String> visitedIfaces = new HashSet<>(allInterfaces.keySet());
        while (!ifaceQueue.isEmpty()) {
            String iface = ifaceQueue.poll();
            ClassOutline ifaceOutline = findOutline(iface, archiveFile, auxJars);
            if (ifaceOutline != null && ifaceOutline.interfaces() != null) {
                for (String parentIface : ifaceOutline.interfaces()) {
                    String dottedParent = parentIface.replace('/', '.');
                    if (visitedIfaces.add(dottedParent)) {
                        allInterfaces.putIfAbsent(dottedParent, iface);
                        ifaceQueue.add(dottedParent);
                    }
                }
            }
        }

        // Format ASCII tree
        StringBuilder sb = new StringBuilder();
        sb.append("Class Hierarchy for ").append(currentClass).append(":\n\n");

        sb.append("Superclass Chain:\n");
        String indent = "";
        for (int i = 0; i < superChain.size(); i++) {
            if (i == 0) {
                sb.append(superChain.get(i)).append("\n");
            } else {
                sb.append(indent).append("\\-- ").append(superChain.get(i)).append("\n");
                indent += "    ";
            }
        }

        sb.append("\nAll Implemented Interfaces (Direct & Inherited):\n");
        if (allInterfaces.isEmpty()) {
            sb.append("  (None)\n");
        } else {
            List<Map.Entry<String, String>> sortedIfaces = new ArrayList<>(allInterfaces.entrySet());
            for (int i = 0; i < sortedIfaces.size(); i++) {
                var entry = sortedIfaces.get(i);
                boolean isLast = (i == sortedIfaces.size() - 1);
                sb.append("  ").append(isLast ? "\\-- " : "|-- ")
                        .append(entry.getKey())
                        .append(" (via ").append(entry.getValue()).append(")\n");
            }
        }

        return new ClassHierarchyResult(currentClass, superChain, allInterfaces, sb.toString().trim());
    }

    private static ClassOutline findOutline(String internalName, File archiveFile, List<File> auxJars) {
        String clean = internalName.replace('.', '/').replaceAll("^/+", "") + ".class";

        // 1. Try archiveFile
        if (archiveFile != null && archiveFile.exists()) {
            try {
                byte[] bytes = ZipBytecodeLoader.loadClassBytes(archiveFile, clean);
                return AsmClassOutlineExtractor.extract(bytes);
            } catch (Exception ignored) {}
        }

        // 2. Try auxiliary jars
        for (File aux : auxJars) {
            if (aux != null && aux.exists()) {
                try {
                    byte[] bytes = ZipBytecodeLoader.loadClassBytes(aux, clean);
                    return AsmClassOutlineExtractor.extract(bytes);
                } catch (Exception ignored) {}
            }
        }

        // 3. Try JVM SystemClassLoader
        try (var is = ClassLoader.getSystemResourceAsStream(clean)) {
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                return AsmClassOutlineExtractor.extract(bytes);
            }
        } catch (Exception ignored) {}

        return null;
    }
}
