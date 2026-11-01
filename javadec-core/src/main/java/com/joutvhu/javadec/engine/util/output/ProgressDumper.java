package com.joutvhu.javadec.engine.util.output;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;

public interface ProgressDumper {
    void analysingType(JavaTypeInstance type);
    void analysingPath(String path);
}
