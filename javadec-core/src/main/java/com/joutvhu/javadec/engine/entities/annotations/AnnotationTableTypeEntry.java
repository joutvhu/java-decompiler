package com.joutvhu.javadec.engine.entities.annotations;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.entities.attributes.TypeAnnotationEntryKind;
import com.joutvhu.javadec.engine.entities.attributes.TypeAnnotationEntryValue;
import com.joutvhu.javadec.engine.entities.attributes.TypeAnnotationTargetInfo;
import com.joutvhu.javadec.engine.entities.attributes.TypePath;

import java.util.Map;

public class AnnotationTableTypeEntry<T extends TypeAnnotationTargetInfo> extends AnnotationTableEntry {
    private final TypeAnnotationEntryValue value;
    private final T targetInfo;
    private final TypePath typePath;

    public AnnotationTableTypeEntry(TypeAnnotationEntryValue value, T targetInfo, TypePath typePath, JavaTypeInstance type, Map<String, ElementValue> elementValueMap) {
        super(type, elementValueMap);
        this.value = value;
        this.targetInfo = targetInfo;
        this.typePath = typePath;
    }

    public TypePath getTypePath() {
        return typePath;
    }

    public TypeAnnotationEntryValue getValue() { return value; }

    public TypeAnnotationEntryKind getKind() {
        return value.getKind();
    }

    public T getTargetInfo() {
        return targetInfo;
    }
}