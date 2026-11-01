package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.transformers;

import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredScope;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.Block;

public class UnusedAnonymousBlockFlattener implements StructuredStatementTransformer {
    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        if (in instanceof Block) {
            ((Block)in).flattenOthersIn();
        }
        in.transformStructuredChildren(this, scope);
        return in;
    }
}
