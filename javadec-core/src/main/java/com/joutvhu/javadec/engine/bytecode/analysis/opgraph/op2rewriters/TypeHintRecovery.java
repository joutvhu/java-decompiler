package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op2rewriters;

import com.joutvhu.javadec.engine.bytecode.analysis.types.discovery.InferredJavaType;

public interface TypeHintRecovery {
    void improve(InferredJavaType type);
}
