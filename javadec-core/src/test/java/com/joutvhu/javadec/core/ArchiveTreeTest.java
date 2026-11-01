package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.model.ArchiveTree;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ArchiveTreeTest {

    @Test
    public void testArchiveTreeBuildAndDepth() {
        Map<String, Long> entries = new LinkedHashMap<>();
        entries.put("com/example/service/A.class", 1024L);
        entries.put("com/example/service/B.class", 2048L);
        entries.put("com/example/model/User.class", 512L);
        entries.put("application.yml", 128L);
        entries.put("META-INF/MANIFEST.MF", 64L);

        ArchiveTree tree = ArchiveTree.build("test.jar", 3776L, entries, null, 2);
        assertNotNull(tree);
        assertEquals(3, tree.totalClasses());
        assertEquals(2, tree.totalResources());
        assertTrue(tree.topLevelPackages().contains("com.example"));

        String formatted = tree.formattedTree();
        assertTrue(formatted.contains("test.jar"));
        assertTrue(formatted.contains("Packages: com.example"));
        assertTrue(formatted.contains("Key resources: META-INF/MANIFEST.MF, application.yml"));
    }

    @Test
    public void testArchiveTreeSubPathFiltering() {
        Map<String, Long> entries = new LinkedHashMap<>();
        entries.put("org/springframework/core/io/Resource.class", 1024L);
        entries.put("org/springframework/web/Controller.class", 2048L);
        entries.put("META-INF/spring.factories", 256L);

        // Filter with dot notation
        ArchiveTree tree = ArchiveTree.build("spring.jar", 3328L, entries, "org.springframework.core", 3);
        assertEquals(1, tree.totalClasses());
        assertTrue(tree.formattedTree().contains("Resource.class"));
        assertFalse(tree.formattedTree().contains("Controller.class"));
    }

    @Test
    public void testSpringBootPrefixHandlingInTree() {
        Map<String, Long> entries = new LinkedHashMap<>();
        entries.put("BOOT-INF/classes/com/company/app/App.class", 1024L);
        entries.put("BOOT-INF/classes/com/company/app/Config.class", 1024L);
        entries.put("BOOT-INF/classes/application.properties", 128L);
        entries.put("META-INF/MANIFEST.MF", 64L);

        ArchiveTree tree = ArchiveTree.build("app.jar", 2240L, entries, null, 3);
        // Ensure package is com.company, NOT BOOT-INF.classes!
        assertTrue(tree.topLevelPackages().contains("com.company"));
        assertFalse(tree.topLevelPackages().contains("BOOT-INF.classes"));

        // Filter with package path on Spring Boot jar
        ArchiveTree scopedTree = ArchiveTree.build("app.jar", 2240L, entries, "com/company/app", 2);
        assertEquals(2, scopedTree.totalClasses());
        assertTrue(scopedTree.formattedTree().contains("App.class"));
    }

    @Test
    public void testSiblingTruncationOverFifteenItems() {
        Map<String, Long> entries = new LinkedHashMap<>();
        for (int i = 0; i < 20; i++) {
            entries.put("com/huge/Class" + i + ".class", 100L);
        }

        ArchiveTree tree = ArchiveTree.build("huge.jar", 2000L, entries, null, 3);
        String formatted = tree.formattedTree();
        // Over 15 items in a single directory should be capped with a summary
        assertTrue(formatted.contains("more items"));
    }
}
