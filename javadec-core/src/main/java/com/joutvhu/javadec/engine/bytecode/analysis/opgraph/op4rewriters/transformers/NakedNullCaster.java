package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.transformers;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.LValue;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.StatementContainer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.*;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.lvalue.StackSSALabel;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.CloneHelper;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.SSAIdentifiers;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredScope;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.bytecode.analysis.types.discovery.InferredJavaType;
import com.joutvhu.javadec.engine.util.collections.MapFactory;

import java.util.Map;

public class NakedNullCaster implements StructuredStatementTransformer, ExpressionRewriter {

    public void transform(Op04StructuredStatement root) {
        StructuredScope structuredScope = new StructuredScope();
        root.transform(this, structuredScope);
    }

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        in.transformStructuredChildren(this, scope);
        in.rewriteExpressions(this);
        return in;
    }

    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if (expression instanceof MemberFunctionInvokation) {
            MemberFunctionInvokation invokation = (MemberFunctionInvokation)expression;
            Expression object = invokation.getObject();
            if (Literal.NULL.equals(object)) {
                // Needs a cast.
                JavaTypeInstance callType = invokation.getMethodPrototype().getClassType();
                // if calltype is null, there's not a lot we can do here.
                if (callType != null) {
                    Map<Expression, Expression> replace = MapFactory.newMap();
                    replace.put(object, new CastExpression(BytecodeLoc.NONE, new InferredJavaType(callType, InferredJavaType.Source.LITERAL), object));
                    CloneHelper cloneHelper = new CloneHelper(replace);
                    expression = expression.deepClone(cloneHelper);
                }
            }
        }
        return expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
    }

    @Override
    public ConditionalExpression rewriteExpression(ConditionalExpression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        Expression res = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
        return (ConditionalExpression) res;
    }

    @Override
    public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return lValue;
    }

    @Override
    public StackSSALabel rewriteExpression(StackSSALabel lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return lValue;
    }

    @Override
    public void handleStatement(StatementContainer statementContainer) {
        // None of that.
    }
}
