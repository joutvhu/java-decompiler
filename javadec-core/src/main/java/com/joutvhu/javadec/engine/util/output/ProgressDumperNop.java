package com.joutvhu.javadec.engine.util.output;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;

public class ProgressDumperNop implements ProgressDumper {
    public static final ProgressDumper INSTANCE = new ProgressDumperNop();

    private ProgressDumperNop() {}

    @Override
    public void analysingType(JavaTypeInstance type) {
    }

    @Override
    public void analysingPath(String path) {
    }
}
