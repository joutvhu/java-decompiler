package com.joutvhu.javadec.engine.util.output;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.entities.Method;

public class NopSummaryDumper implements SummaryDumper {

    @Override
    public void notify(String message) {
    }

    @Override
    public void notifyError(JavaTypeInstance controllingType, Method method, String error) {
    }

    @Override
    public void close() {
    }
}
