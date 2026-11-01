package com.joutvhu.javadec.engine.util.output;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.util.getopt.OptionsImpl;

public class ProgressDumperStdErr implements ProgressDumper {

    @Override
    public void analysingType(JavaTypeInstance type) {
        System.err.println("Processing " + type.getRawName());
    }

    @Override
    public void analysingPath(String path) {
        System.err.println("Processing " + path + " (use " + OptionsImpl.SILENT.getName() + " to silence)");
    }
}
