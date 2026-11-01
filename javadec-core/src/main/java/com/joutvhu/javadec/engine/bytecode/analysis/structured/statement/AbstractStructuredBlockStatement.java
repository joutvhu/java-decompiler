package com.joutvhu.javadec.engine.bytecode.analysis.structured.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredScope;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;

public abstract class AbstractStructuredBlockStatement extends AbstractStructuredStatement {
    private Op04StructuredStatement body;

    AbstractStructuredBlockStatement(BytecodeLoc loc, Op04StructuredStatement body) {
        super(loc);
        this.body = body;
    }

    public Op04StructuredStatement getBody() {
        return body;
    }

    @Override
    public boolean isRecursivelyStructured() {
        return body.isFullyStructured();
    }

    @Override
    public void transformStructuredChildren(StructuredStatementTransformer transformer, StructuredScope scope) {
        getBody().transform(transformer, scope);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        if (!collector.isStatementRecursive()) return;
        body.collectTypeUsages(collector);
    }
}
