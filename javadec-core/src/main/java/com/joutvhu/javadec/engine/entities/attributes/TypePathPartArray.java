package com.joutvhu.javadec.engine.entities.attributes;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaAnnotatedTypeIterator;
import com.joutvhu.javadec.engine.util.DecompilerComments;

public class TypePathPartArray implements TypePathPart {
    public static final TypePathPartArray INSTANCE = new TypePathPartArray();

    private TypePathPartArray() {
    }

    @Override
    public JavaAnnotatedTypeIterator apply(JavaAnnotatedTypeIterator it, DecompilerComments comments) {
        return it.moveArray(comments);
    }
}
