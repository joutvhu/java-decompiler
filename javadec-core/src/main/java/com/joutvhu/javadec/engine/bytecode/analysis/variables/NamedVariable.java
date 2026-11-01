package com.joutvhu.javadec.engine.bytecode.analysis.variables;

import com.joutvhu.javadec.engine.util.output.Dumpable;
import com.joutvhu.javadec.engine.util.output.Dumper;

public interface NamedVariable extends Dumpable {
    void forceName(String name);

    String getStringName();

    boolean isGoodName();

    @Override
    Dumper dump(Dumper d);

    Dumper dump(Dumper d, boolean defines);
}
