package com.joutvhu.javadec.engine.bytecode.analysis.parse.expression;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.StatementContainer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.misc.Precedence;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.CloneHelper;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriterHelper;
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
 * A static call that doesn't necessarily exist, for a type we don't necessarily have.
 */
public class StaticFunctionInvokationExplicit extends AbstractFunctionInvokationExplicit {
    public StaticFunctionInvokationExplicit(BytecodeLoc loc, InferredJavaType res, JavaTypeInstance clazz, String method, List<Expression> args) {
        super(loc, res, clazz, method, args);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, getArgs());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof StaticFunctionInvokationExplicit)) return false;
        StaticFunctionInvokationExplicit other = (StaticFunctionInvokationExplicit)o;
        return getClazz().equals(other.getClazz()) && getMethod().equals(other.getMethod()) && getArgs().equals(other.getArgs());
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.WEAKEST;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        d.dump(getClazz()).separator(".").print(getMethod()).separator("(");
        boolean first = true;
        for (Expression arg : getArgs()) {
            first = StringUtils.comma(first, d);
            d.dump(arg);
        }
        d.separator(")");
        return d;
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == this) return true;
        if (o == null) return false;
        if (!(o instanceof StaticFunctionInvokationExplicit)) return false;
        StaticFunctionInvokationExplicit other = (StaticFunctionInvokationExplicit)o;
        if (!constraint.equivalent(getMethod(), other.getMethod())) return false;
        if (!constraint.equivalent(getClazz(), other.getClazz())) return false;
        if (!constraint.equivalent(getArgs(), other.getArgs())) return false;
        return true;
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new StaticFunctionInvokationExplicit(getLoc(), getInferredJavaType(), getClazz(), getMethod(), cloneHelper.replaceOrClone(getArgs()));
    }
}
