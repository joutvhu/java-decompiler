package com.joutvhu.javadec.engine.entities.attributes;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaAnnotatedTypeIterator;
import com.joutvhu.javadec.engine.util.DecompilerComments;

public class TypePathPartParameterized implements TypePathPart {
    private final int index;

    public TypePathPartParameterized(int index) {
        this.index = index;
    }

    @Override
    public JavaAnnotatedTypeIterator apply(JavaAnnotatedTypeIterator it, DecompilerComments comments) {
        return it.moveParameterized(index, comments);
    }
}
