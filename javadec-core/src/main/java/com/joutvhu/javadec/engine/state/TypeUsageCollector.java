package com.joutvhu.javadec.engine.state;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaRefTypeInstance;
import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.util.TypeUsageCollectable;

import java.util.Collection;

public interface TypeUsageCollector {
    void collectRefType(JavaRefTypeInstance type);

    void collect(JavaTypeInstance type);

    void collect(Collection<? extends JavaTypeInstance> types);

    // Explicitly named helper to allow J8's less lenient bridging.
    void collectFromT(TypeUsageCollectable collectable);

    void collectFrom(TypeUsageCollectable collectable);

    void collectFrom(Collection<? extends TypeUsageCollectable> collectables);

    TypeUsageInformation getTypeUsageInformation();

    boolean isStatementRecursive();
}
