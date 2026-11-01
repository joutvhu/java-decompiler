package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.checker;

import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import com.joutvhu.javadec.engine.util.DecompilerComments;

public interface Op04Checker extends StructuredStatementTransformer {
    void commentInto(DecompilerComments comments);
}
