package com.joutvhu.javadec.core.loader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Resolves auxiliary dependencies and classpath JARs for the decompiler.
 * Automatically identifies dependencies from:
 * 1. Companion .pom files in ~/.m2/repository
 * 2. Embedded META-INF/maven/.../pom.xml files inside the JAR
 * 3. Embedded BOOT-INF/lib/*.jar and WEB-INF/lib/*.jar libraries
 * 4. User-supplied manual classpath arguments (-cp / --classpath)
 */
public class AuxiliaryClasspathResolver {

    /**
     * Resolves all auxiliary JARs for the given primary JAR and options.
     */
    public static List<File> resolve(File primaryJar, String manualClasspath, boolean autoDetect) {
        Set<File> result = new LinkedHashSet<>();

        // 1. Resolve manual classpath if provided
        if (manualClasspath != null && !manualClasspath.isBlank()) {
            resolveManualClasspath(manualClasspath, result);
        }

        // 2. Automatically detect dependencies if enabled
        if (autoDetect && primaryJar != null && primaryJar.exists() && primaryJar.isFile()) {
            resolveAutomaticDependencies(primaryJar, result);
        }

        return new ArrayList<>(result);
    }

    private static void resolveManualClasspath(String cp, Set<File> result) {
        // Split by ';' or ':' (avoid splitting on Windows drive letter 'C:')
        String[] entries = cp.split("(?<!^[a-zA-Z]):|;");
        for (String entry : entries) {
            String path = entry.trim();
            if (path.isEmpty()) continue;

            if (MavenCoordinateResolver.isMavenCoordinate(path)) {
                try {
                    File resolved = MavenCoordinateResolver.resolveCoordinate(path);
                    if (resolved != null && resolved.exists()) {
                        result.add(resolved);
                        continue;
                    }
                } catch (Exception ignored) {}
            }

            File file = PathUtil.resolveFile(path);
            if (file != null && file.exists()) {
                if (file.isDirectory()) {
                    // Add all .jar files in directory
                    File[] jars = file.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
                    if (jars != null) {
                        result.addAll(Arrays.asList(jars));
                    }
                } else if (file.isFile()) {
                    result.add(file);
                }
            }
        }
    }

    private static void resolveAutomaticDependencies(File primaryJar, Set<File> result) {
        MavenSettings settings = MavenSettings.load();
        String localRepo = settings.getLocalRepositoryPath();

        // A. Check for companion .pom in the same directory (e.g. in ~/.m2/repository/...)
        File parentDir = primaryJar.getParentFile();
        if (parentDir != null && parentDir.exists()) {
            String jarName = primaryJar.getName();
            if (jarName.endsWith(".jar")) {
                String pomName = jarName.substring(0, jarName.length() - 4) + ".pom";
                File companionPom = new File(parentDir, pomName);
                if (companionPom.exists() && companionPom.isFile()) {
                    parsePomAndCollectJars(companionPom, localRepo, result);
                }
            }
        }

        // B. Check for embedded META-INF/maven/**/pom.xml inside the JAR
        try (ZipFile zip = new ZipFile(primaryJar)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("META-INF/maven/") && name.endsWith("/pom.xml")) {
                    try (InputStream is = zip.getInputStream(entry)) {
                        parsePomStreamAndCollectJars(is, localRepo, result);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
    }

    private static void parsePomAndCollectJars(File pomFile, String localRepo, Set<File> result) {
        try (InputStream is = new FileInputStream(pomFile)) {
            parsePomStreamAndCollectJars(is, localRepo, result);
        } catch (Exception ignored) {}
    }

    private static void parsePomStreamAndCollectJars(InputStream pomStream, String localRepo, Set<File> result) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(pomStream);
            doc.getDocumentElement().normalize();

            // Extract POM properties for placeholder replacement (e.g. ${spring.version})
            Map<String, String> properties = new HashMap<>();
            NodeList propNodes = doc.getElementsByTagName("properties");
            if (propNodes.getLength() > 0) {
                Element propEl = (Element) propNodes.item(0);
                NodeList children = propEl.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    if (children.item(i) instanceof Element child) {
                        properties.put(child.getTagName(), child.getTextContent().trim());
                    }
                }
            }

            // Extract dependencies
            NodeList depNodes = doc.getElementsByTagName("dependency");
            for (int i = 0; i < depNodes.getLength(); i++) {
                Element depEl = (Element) depNodes.item(i);
                String groupId = getChildText(depEl, "groupId");
                String artifactId = getChildText(depEl, "artifactId");
                String version = getChildText(depEl, "version");
                String scope = getChildText(depEl, "scope");

                // Skip test scope dependencies
                if ("test".equalsIgnoreCase(scope)) continue;

                if (groupId != null && artifactId != null && version != null) {
                    version = resolvePlaceholders(version, properties);
                    if (!version.startsWith("${")) { // only if placeholder resolved
                        String groupPath = groupId.replace('.', '/');
                        File candidate = new File(localRepo, groupPath + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar");
                        if (candidate.exists() && candidate.isFile()) {
                            result.add(candidate);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static String resolvePlaceholders(String val, Map<String, String> props) {
        if (val == null || !val.contains("${")) return val;
        Matcher m = Pattern.compile("\\$\\{([^}]+)}").matcher(val);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            String replacement = props.getOrDefault(key, m.group(0));
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String getChildText(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            return list.item(0).getTextContent().trim();
        }
        return null;
    }
}
