package com.joutvhu.javadec.engine.entities.attributes;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaAnnotatedTypeIterator;
import com.joutvhu.javadec.engine.util.DecompilerComments;

public class TypePathPartBound implements TypePathPart {
    public static final TypePathPartBound INSTANCE = new TypePathPartBound();

    private TypePathPartBound() {
    }

    @Override
    public JavaAnnotatedTypeIterator apply(JavaAnnotatedTypeIterator it, DecompilerComments comments) {
        return it.moveBound(comments);
    }
}
