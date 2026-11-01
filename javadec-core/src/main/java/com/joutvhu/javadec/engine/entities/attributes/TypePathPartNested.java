package com.joutvhu.javadec.engine.entities.attributes;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaAnnotatedTypeIterator;
import com.joutvhu.javadec.engine.util.DecompilerComments;

public class TypePathPartNested implements TypePathPart {
    public static final TypePathPartNested INSTANCE = new TypePathPartNested();

    private TypePathPartNested() {
    }

    @Override
    public JavaAnnotatedTypeIterator apply(JavaAnnotatedTypeIterator it, DecompilerComments comments) {
        return it.moveNested(comments);
    }
}
