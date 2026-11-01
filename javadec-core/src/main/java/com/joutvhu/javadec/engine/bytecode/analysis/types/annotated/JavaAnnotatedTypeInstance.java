package com.joutvhu.javadec.engine.bytecode.analysis.types.annotated;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaAnnotatedTypeIterator;
import com.joutvhu.javadec.engine.util.output.Dumpable;

public interface JavaAnnotatedTypeInstance extends Dumpable {
    JavaAnnotatedTypeIterator pathIterator();
}
