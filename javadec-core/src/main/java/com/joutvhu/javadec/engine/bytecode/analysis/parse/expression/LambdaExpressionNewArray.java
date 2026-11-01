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
import com.joutvhu.javadec.engine.bytecode.analysis.types.discovery.InferredJavaType;
import com.joutvhu.javadec.engine.util.MiscConstants;
import com.joutvhu.javadec.engine.util.output.Dumper;

/*
 * It would be nice to roll this into the standard lambda expressions, however new arrays
 * don't have a method reference, as this is a bytecode primitive.
 */
public class LambdaExpressionNewArray extends AbstractExpression implements LambdaExpressionCommon {
    private final InferredJavaType constrType;

    public LambdaExpressionNewArray(BytecodeLoc loc, InferredJavaType resType, InferredJavaType constrType) {
        super(loc, resType);
        this.constrType = constrType;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return getLoc();
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        return d.dump(constrType.getJavaTypeInstance()).print("::").methodName(MiscConstants.NEW, null,true, false);
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        return this;
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return this;
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        return this.equals(o);
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LambdaExpressionNewArray)) return false;
        LambdaExpressionNewArray other = (LambdaExpressionNewArray)o;
        return other.getInferredJavaType().getJavaTypeInstance().equals(getInferredJavaType().getJavaTypeInstance());
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.WEAKEST;
    }
}
