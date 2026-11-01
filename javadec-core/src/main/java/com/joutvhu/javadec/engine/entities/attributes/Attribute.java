package com.joutvhu.javadec.engine.entities.attributes;

import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.KnowsRawName;
import com.joutvhu.javadec.engine.util.TypeUsageCollectable;
import com.joutvhu.javadec.engine.util.output.Dumpable;
import com.joutvhu.javadec.engine.util.output.Dumper;

public abstract class Attribute implements KnowsRawName, Dumpable, TypeUsageCollectable {

    @Override
    public abstract Dumper dump(Dumper d);

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
    }
}
