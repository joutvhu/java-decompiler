package com.joutvhu.javadec.engine.bytecode.analysis.structured.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredScope;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.List;

public class StructuredExpressionYield extends AbstractStructuredStatement {

    private Expression value;

    public StructuredExpressionYield(BytecodeLoc loc, Expression value) {
        super(loc);
        this.value = value;
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.print("yield ").dump(value).endCodeln();
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        value.collectTypeUsages(collector);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, value);
    }

    @Override
    public void transformStructuredChildren(StructuredStatementTransformer transformer, StructuredScope scope) {
    }

    @Override
    public void linearizeInto(List<StructuredStatement> out) {
        out.add(this);
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        StructuredStatement o = matchIterator.getCurrent();
        if (!(o instanceof StructuredExpressionYield)) return false;
        StructuredExpressionYield other = (StructuredExpressionYield) o;
        if (!value.equals(other.value)) return false;
        // Don't check locality.
        matchIterator.advance();
        return true;
    }

    public Expression getValue() {
        return value;
    }

    @Override
    public void traceLocalVariableScope(LValueScopeDiscoverer scopeDiscoverer) {
        value.collectUsedLValues(scopeDiscoverer);
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter) {
        value = expressionRewriter.rewriteExpression(value, null, this.getContainer(), null);
    }
}
