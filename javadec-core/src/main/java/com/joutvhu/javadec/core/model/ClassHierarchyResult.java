package com.joutvhu.javadec.core.model;

import java.util.List;
import java.util.Map;

/**
 * Result of traversing the complete inheritance hierarchy and interface graph of a class.
 */
public record ClassHierarchyResult(
        String className,
        List<String> superclassChain,
        Map<String, String> implementedInterfaces, // interfaceName -> viaClass
        String formattedHierarchy
) {}
