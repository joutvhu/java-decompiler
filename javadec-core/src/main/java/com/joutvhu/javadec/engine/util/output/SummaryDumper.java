package com.joutvhu.javadec.engine.util.output;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.entities.Method;

public interface SummaryDumper {
    void notify(String message);

    void notifyError(JavaTypeInstance controllingType, Method method, String error);

    void close();
}
