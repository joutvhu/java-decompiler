package com.joutvhu.javadec.engine.bytecode.analysis.types;

import com.joutvhu.javadec.engine.entities.annotations.AnnotationTableEntry;
import com.joutvhu.javadec.engine.entities.attributes.AttributeMap;
import com.joutvhu.javadec.engine.entities.attributes.AttributeRuntimeInvisibleAnnotations;
import com.joutvhu.javadec.engine.entities.attributes.AttributeRuntimeVisibleAnnotations;
import com.joutvhu.javadec.engine.util.collections.ListFactory;

import java.util.List;

public class MiscAnnotations {
    public static List<AnnotationTableEntry> BasicAnnotations(AttributeMap attributeMap) {
        AttributeRuntimeVisibleAnnotations a1 = attributeMap.getByName(AttributeRuntimeVisibleAnnotations.ATTRIBUTE_NAME);
        AttributeRuntimeInvisibleAnnotations a2 = attributeMap.getByName(AttributeRuntimeInvisibleAnnotations.ATTRIBUTE_NAME);
        List<AnnotationTableEntry> e1 = a1 == null ? null : a1.getEntryList();
        List<AnnotationTableEntry> e2 = a2 == null ? null : a2.getEntryList();
        return ListFactory.combinedOptimistic(e1,e2);
    }

}
