package com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.rewriteinterface;

import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.VarArgsRewriter;

public interface FunctionProcessor {
    // This feels like it should be refactored into a generalised visitor interface
    void rewriteVarArgs(VarArgsRewriter varArgsRewriter);
}
