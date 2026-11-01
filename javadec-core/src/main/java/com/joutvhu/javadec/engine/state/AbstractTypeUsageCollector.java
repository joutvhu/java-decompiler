package com.joutvhu.javadec.engine.state;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.util.TypeUsageCollectable;

import java.util.Collection;

public abstract class AbstractTypeUsageCollector implements TypeUsageCollector {
    @Override
    public void collect(Collection<? extends JavaTypeInstance> types) {
        if (types == null) return;
        for (JavaTypeInstance type : types) collect(type);
    }

    @Override
    public void collectFrom(TypeUsageCollectable collectable) {
        if (collectable != null) collectable.collectTypeUsages(this);
    }

    @Override
    public void collectFromT(TypeUsageCollectable collectable) {
        collectFrom(collectable);
    }

    @Override
    public void collectFrom(Collection<? extends TypeUsageCollectable> collectables) {
        if (collectables != null) {
            for (TypeUsageCollectable collectable : collectables) {
                if (collectable != null) collectable.collectTypeUsages(this);
            }
        }
    }
}
