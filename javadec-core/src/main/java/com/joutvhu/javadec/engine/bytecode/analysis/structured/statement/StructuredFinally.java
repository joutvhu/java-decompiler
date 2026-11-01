package com.joutvhu.javadec.engine.bytecode.analysis.structured.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredScope;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.List;

public class StructuredFinally extends AbstractStructuredStatement {
    private final Op04StructuredStatement catchBlock;

    public StructuredFinally(Op04StructuredStatement catchBlock) {
        super(BytecodeLoc.NONE);
        this.catchBlock = catchBlock;
    }

    @Override
    public Dumper dump(Dumper dumper) {
        dumper.keyword("finally ");
        catchBlock.dump(dumper);
        return dumper;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return getLoc();
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collectFrom(catchBlock);
    }

    @Override
    public boolean isProperlyStructured() {
        return true;
    }

    @Override
    public boolean fallsNopToNext() {
        return true;
    }

    @Override
    public boolean isScopeBlock() {
        return true;
    }

    @Override
    public void transformStructuredChildren(StructuredStatementTransformer transformer, StructuredScope scope) {
        catchBlock.transform(transformer, scope);
    }

    @Override
    public void linearizeInto(List<StructuredStatement> out) {
        out.add(this);
        catchBlock.linearizeStatementsInto(out);
    }

    public Op04StructuredStatement getCatchBlock() {
        return catchBlock;
    }

    @Override
    public boolean isRecursivelyStructured() {
        return catchBlock.isFullyStructured();
    }


    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        StructuredStatement o = matchIterator.getCurrent();
        if (!(o instanceof StructuredFinally)) return false;
        StructuredFinally other = (StructuredFinally) o;
        // we don't actually check any equality for a match.
        matchIterator.advance();
        return true;
    }

    @Override
    public void traceLocalVariableScope(LValueScopeDiscoverer scopeDiscoverer) {
        scopeDiscoverer.processOp04Statement(catchBlock);
    }


    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter) {
    }
}
