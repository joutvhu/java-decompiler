package com.joutvhu.javadec.engine.entities.attributes;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaAnnotatedTypeIterator;
import com.joutvhu.javadec.engine.util.DecompilerComments;

public interface TypePathPart {
    JavaAnnotatedTypeIterator apply(JavaAnnotatedTypeIterator it, DecompilerComments comments);
}
