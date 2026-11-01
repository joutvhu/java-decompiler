package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.transformers;

import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredScope;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;

public interface StructuredStatementTransformer {
    StructuredStatement transform(StructuredStatement in, StructuredScope scope);
}
