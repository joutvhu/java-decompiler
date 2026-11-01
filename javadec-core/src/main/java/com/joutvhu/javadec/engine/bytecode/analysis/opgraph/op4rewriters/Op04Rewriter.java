package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters;

import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op04StructuredStatement;

public interface Op04Rewriter {
    void rewrite(Op04StructuredStatement root);
}
