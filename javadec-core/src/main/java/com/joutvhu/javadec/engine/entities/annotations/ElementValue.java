package com.joutvhu.javadec.engine.entities.annotations;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.util.TypeUsageCollectable;
import com.joutvhu.javadec.engine.util.output.Dumpable;

public interface ElementValue extends Dumpable, TypeUsageCollectable {
    ElementValue withTypeHint(JavaTypeInstance hint);
}
