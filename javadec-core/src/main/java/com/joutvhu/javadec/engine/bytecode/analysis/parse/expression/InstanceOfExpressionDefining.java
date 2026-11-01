package com.joutvhu.javadec.engine.bytecode.analysis.parse.expression;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.LValue;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Pattern;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.StatementContainer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.misc.Precedence;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.CloneHelper;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.EquivalenceConstraint;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.LValueRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.LValueUsageCollector;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.SSAIdentifiers;
import com.joutvhu.javadec.engine.bytecode.analysis.types.discovery.InferredJavaType;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.Troolean;
import com.joutvhu.javadec.engine.util.collections.SetFactory;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.Set;

public class InstanceOfExpressionDefining extends AbstractExpression implements ConditionalExpression {
    private Expression lhs;
    private final Pattern pattern;

    public InstanceOfExpressionDefining(BytecodeLoc loc, InferredJavaType inferredJavaType, Expression lhs, Pattern pattern) {
        super(loc, inferredJavaType);
        this.lhs = lhs;
        this.pattern = pattern;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, lhs);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        lhs.collectTypeUsages(collector);
        pattern.collectTypeUsages(collector);
    }

    public InstanceOfExpressionDefining withReplacedExpression(Expression e) {
        return new InstanceOfExpressionDefining(getLoc(), this.getInferredJavaType(), e, pattern);
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new InstanceOfExpressionDefining(getLoc(), getInferredJavaType(), cloneHelper.replaceOrClone(lhs), pattern);
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.REL_CMP_INSTANCEOF;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        lhs.dumpWithOuterPrecedence(d, getPrecedence(), Troolean.NEITHER);
        d.print(" instanceof ");
        pattern.dump(d, true);
        return d;
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        lhs = lhs.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        return this;
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        lhs = expressionRewriter.rewriteExpression(lhs, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return applyExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
    }


    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        lhs.collectUsedLValues(lValueUsageCollector);
    }

    @Override
    public ConditionalExpression getNegated() {
        return new NotOperation(BytecodeLoc.NONE, this);
    }

    @Override
    public int getSize(Precedence outerPrecedence) {
        return 1;
    }

    @Override
    public ConditionalExpression getDemorganApplied(boolean amNegating) {
        return amNegating ? getNegated() : this;
    }

    @Override
    public ConditionalExpression getRightDeep() {
        return this;
    }

    @Override
    public Set<LValue> getLoopLValues() {
        Set<LValue> res = SetFactory.newSet();
        if (lhs instanceof LValueExpression) {
            res.add(((LValueExpression) lhs).getLValue());
        }
        return res;
    }

    @Override
    public ConditionalExpression optimiseForType() {
        return this;
    }

    @Override
    public ConditionalExpression simplify() {
        return this;
    }

    public Expression getLhs() {
        return lhs;
    }

    public Pattern getPattern() {
        return pattern;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (!(o instanceof InstanceOfExpressionDefining)) return false;
        InstanceOfExpressionDefining other = (InstanceOfExpressionDefining) o;
        if (!lhs.equals(other.lhs)) return false;
        if (!pattern.equals(other.pattern)) return false;
        return true;
    }

    @Override
    public final boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) return false;
        if (o == this) return true;
        if (getClass() != o.getClass()) return false;
        InstanceOfExpressionDefining other = (InstanceOfExpressionDefining) o;
        if (!constraint.equivalent(lhs, other.lhs)) return false;
        if (!constraint.equivalent(pattern, other.pattern)) return false;
        return true;
    }

}
