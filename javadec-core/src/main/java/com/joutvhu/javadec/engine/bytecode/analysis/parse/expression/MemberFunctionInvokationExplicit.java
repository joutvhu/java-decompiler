package com.joutvhu.javadec.engine.bytecode.analysis.parse.expression;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.StatementContainer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.misc.Precedence;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.CloneHelper;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.EquivalenceConstraint;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.LValueRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.LValueUsageCollector;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.SSAIdentifiers;
import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.bytecode.analysis.types.discovery.InferredJavaType;
import com.joutvhu.javadec.engine.util.StringUtils;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.List;

/**
 * A member function call that doesn't necessarily exist, for a type we don't necessarily have.
 */
public class MemberFunctionInvokationExplicit extends AbstractFunctionInvokationExplicit {
    private Expression object;

    MemberFunctionInvokationExplicit(BytecodeLoc loc, InferredJavaType res, JavaTypeInstance clazz, Expression object, String method, List<Expression> args) {
        super(loc, res, clazz, method, args);
        this.object = object;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, object);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof MemberFunctionInvokationExplicit)) return false;
        MemberFunctionInvokationExplicit other = (MemberFunctionInvokationExplicit)o;
        return getClazz().equals(other.getClazz()) && object.equals(other.object) && getMethod().equals(other.getMethod()) && getArgs().equals(other.getArgs());
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.WEAKEST;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        d.dump(object).separator(".").print(getMethod()).separator("(");
        boolean first = true;
        for (Expression arg : getArgs()) {
            first = StringUtils.comma(first, d);
            d.dump(arg);
        }
        d.separator(")");
        return d;
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        object = object.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        return super.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        object = object.applyExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
        super.applyExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        super.applyReverseExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
        object = object.applyExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        object.collectUsedLValues(lValueUsageCollector);
        super.collectUsedLValues(lValueUsageCollector);
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == this) return true;
        if (o == null) return false;
        if (!(o instanceof MemberFunctionInvokationExplicit)) return false;
        MemberFunctionInvokationExplicit other = (MemberFunctionInvokationExplicit)o;
        if (!constraint.equivalent(object, other.object)) return false;
        if (!constraint.equivalent(getMethod(), other.getMethod())) return false;
        if (!constraint.equivalent(getClazz(), other.getClazz())) return false;
        if (!constraint.equivalent(getArgs(), other.getArgs())) return false;
        return true;
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new MemberFunctionInvokationExplicit(getLoc(), getInferredJavaType(), getClazz(), object, getMethod(), cloneHelper.replaceOrClone(getArgs()));
    }
}
