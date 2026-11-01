package com.joutvhu.javadec.engine.util;

import com.joutvhu.javadec.engine.state.TypeUsageCollector;

public interface TypeUsageCollectable {
    void collectTypeUsages(TypeUsageCollector collector);
}
