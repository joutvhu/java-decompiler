package com.joutvhu.javadec.engine.bytecode.analysis.stack;

import com.joutvhu.javadec.engine.bytecode.analysis.types.StackTypes;

public interface StackDelta {
    boolean isNoOp();

    StackTypes getConsumed();

    StackTypes getProduced();

    long getChange();
}
