package com.joutvhu.javadec.engine.bytecode.analysis.structured.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.ConditionalExpression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.BlockIdentifier;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;

import java.util.List;

// do / while.
public abstract class AbstractStructuredConditionalLoopStatement extends AbstractStructuredBlockStatement {
    protected ConditionalExpression condition;
    protected final BlockIdentifier block;

    AbstractStructuredConditionalLoopStatement(BytecodeLoc loc, ConditionalExpression condition, BlockIdentifier block, Op04StructuredStatement body) {
        super(loc, body);
        this.condition = condition;
        this.block = block;
    }

    public BlockIdentifier getBlock() {
        return block;
    }

    public ConditionalExpression getCondition() {
        return condition;
    }

    public void setCondition(ConditionalExpression condition) {
        this.condition = condition;
    }

    @Override
    public BlockIdentifier getBreakableBlockOrNull() {
        return block;
    }

    @Override
    public boolean supportsBreak() {
        return true;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collectFrom(condition);
        super.collectTypeUsages(collector);
    }

    public boolean isInfinite() {
        return condition == null;
    }

    @Override
    public boolean isScopeBlock() {
        return true;
    }

    @Override
    public boolean supportsContinueBreak() {
        return true;
    }

    @Override
    public void traceLocalVariableScope(LValueScopeDiscoverer scopeDiscoverer) {
        if (condition != null) {
            condition.collectUsedLValues(scopeDiscoverer);
        }
        scopeDiscoverer.processOp04Statement(getBody());
    }

    @Override
    public void linearizeInto(List<StructuredStatement> out) {
        out.add(this);
        getBody().linearizeStatementsInto(out);
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter) {
        if (condition != null) {
            condition = expressionRewriter.rewriteExpression(condition, null, this.getContainer(), null);
        }
    }

}
