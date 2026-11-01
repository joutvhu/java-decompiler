package com.joutvhu.javadec.engine.bytecode.analysis.structured.expression;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.transformers.ExpressionRewriterTransformer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.LValue;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.StatementContainer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.AbstractExpression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.ConditionalExpression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.misc.Precedence;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.CloneHelper;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.EquivalenceConstraint;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.LValueRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.LValueUsageCollector;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.SSAIdentifiers;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredScope;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.StructuredDefinition;
import com.joutvhu.javadec.engine.bytecode.analysis.types.discovery.InferredJavaType;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.annotation.Nullable;
import com.joutvhu.javadec.engine.util.output.Dumper;

public class StructuredCaseUnassignedExpression extends AbstractExpression {

    public StructuredCaseUnassignedExpression(InferredJavaType inferredJavaType) {
        super(BytecodeLoc.TODO, inferredJavaType);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.TODO;
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return this;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        super.collectTypeUsages(collector);
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return applyExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
    }


    @Override
    public Precedence getPrecedence() {
        return Precedence.WEAKEST;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        d.dump(getInferredJavaType().getJavaTypeInstance());
        d.separator(" ");
        d.print("_");
        return d;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StructuredCaseUnassignedExpression that = (StructuredCaseUnassignedExpression) o;
        return true;
    }

    @Override
    public final boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) return false;
        if (o == this) return true;
        if (getClass() != o.getClass()) return false;
        StructuredCaseUnassignedExpression other = (StructuredCaseUnassignedExpression) o;
        return true;
    }

}
