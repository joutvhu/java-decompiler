package com.joutvhu.javadec.engine.state;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.List;

public interface ObfuscationRewriter {
    Dumper wrap(Dumper d);

    JavaTypeInstance get(JavaTypeInstance t);

    List<JavaTypeInstance> get(List<JavaTypeInstance> types);
}
