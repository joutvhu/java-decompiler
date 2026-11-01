package com.joutvhu.javadec.engine.bytecode.analysis.structured.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.LValue;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.lvalue.LocalVariable;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.BlockIdentifier;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredScope;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import com.joutvhu.javadec.engine.util.functors.Predicate;

import java.util.List;

public abstract class AbstractUnStructuredStatement extends AbstractStructuredStatement {

    public AbstractUnStructuredStatement(BytecodeLoc loc) {
        super(loc);
    }

    @Override
    public final void transformStructuredChildren(StructuredStatementTransformer transformer, StructuredScope scope) {
    }

    @Override
    public final void transformStructuredChildrenInReverse(StructuredStatementTransformer transformer, StructuredScope scope) {
    }

    @Override
    public final boolean isProperlyStructured() {
        return false;
    }

    @Override
    public BlockIdentifier getBreakableBlockOrNull() {
        return null;
    }

    @Override
    public final boolean isRecursivelyStructured() {
        return false;
    }

    @Override
    public void linearizeInto(List<StructuredStatement> out) {
        throw new UnsupportedOperationException("Can't linearise an unstructured statement");
    }

    @Override
    public void traceLocalVariableScope(LValueScopeDiscoverer scopeDiscoverer) {
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter) {
    }

    @Override
    public boolean isEffectivelyNOP() {
        return false;
    }

    @Override
    public List<LValue> findCreatedHere() {
        return null;
    }

    @Override
    public String suggestName(LocalVariable createdHere, Predicate<String> testNameUsedFn) {
        return null;
    }
}
