package com.joutvhu.javadec.engine.bytecode.analysis.parse.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.CastExpression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.CloneHelper;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.*;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.StructuredReturn;
import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaGenericPlaceholderTypeInstance;
import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.bytecode.analysis.types.RawJavaType;
import com.joutvhu.javadec.engine.bytecode.analysis.types.discovery.InferredJavaType;
import com.joutvhu.javadec.engine.entities.exceptions.ExceptionCheck;
import com.joutvhu.javadec.engine.util.output.Dumper;

public class ReturnValueStatement extends ReturnStatement {
    private Expression rvalue;
    private final JavaTypeInstance fnReturnType;

    public ReturnValueStatement(BytecodeLoc loc, Expression rvalue, JavaTypeInstance fnReturnType) {
        super(loc);
        this.rvalue = rvalue;
        if (fnReturnType instanceof JavaGenericPlaceholderTypeInstance) {
            this.rvalue = new CastExpression(BytecodeLoc.NONE, new InferredJavaType(fnReturnType, InferredJavaType.Source.FUNCTION, true), this.rvalue);
        }
        this.fnReturnType = fnReturnType;
    }

    @Override
    public ReturnStatement deepClone(CloneHelper cloneHelper) {
        return new ReturnValueStatement(getLoc(), cloneHelper.replaceOrClone(rvalue), fnReturnType);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, rvalue);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.keyword("return ").dump(rvalue).endCodeln();
    }

    public Expression getReturnValue() {
        return rvalue;
    }

    public JavaTypeInstance getFnReturnType() {
        return fnReturnType;
    }

    @Override
    public void replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers) {
        this.rvalue = rvalue.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, getContainer());
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers) {
        this.rvalue = expressionRewriter.rewriteExpression(rvalue, ssaIdentifiers, getContainer(), ExpressionRewriterFlags.RVALUE);
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
        rvalue.collectUsedLValues(lValueUsageCollector);
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        /*
         * Create explicit cast - a bit late in the day - but we don't always know about int types
         * until now.
         */
        Expression rvalueUse = rvalue;
        if (fnReturnType instanceof RawJavaType) {
            if (!rvalue.getInferredJavaType().getJavaTypeInstance().implicitlyCastsTo(fnReturnType, null)) {
                InferredJavaType inferredJavaType = new InferredJavaType(fnReturnType, InferredJavaType.Source.FUNCTION, true);
                rvalueUse = new CastExpression(BytecodeLoc.NONE, inferredJavaType, rvalue, true);
            }
        }
        return new StructuredReturn(getLoc(), rvalueUse, fnReturnType);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (!(o instanceof ReturnValueStatement)) return false;

        ReturnValueStatement other = (ReturnValueStatement) o;
        return rvalue.equals(other.rvalue);
    }

    @Override
    public final boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) return false;
        if (o == this) return true;
        if (getClass() != o.getClass()) return false;
        ReturnValueStatement other = (ReturnValueStatement) o;
        if (!constraint.equivalent(rvalue, other.rvalue)) return false;
        return true;
    }

    @Override
    public boolean canThrow(ExceptionCheck caught) {
        return rvalue.canThrow(caught);
    }
}
