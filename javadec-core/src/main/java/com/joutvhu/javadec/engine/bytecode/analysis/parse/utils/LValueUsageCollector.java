package com.joutvhu.javadec.engine.bytecode.analysis.parse.utils;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.LValue;

public interface LValueUsageCollector {
    void collect(LValue lValue, ReadWrite rw);
}
