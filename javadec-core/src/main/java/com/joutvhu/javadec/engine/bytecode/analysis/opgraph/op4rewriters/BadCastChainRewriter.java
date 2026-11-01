package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.StatementContainer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.CastExpression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.ConditionalExpression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.Literal;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.TernaryExpression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.SSAIdentifiers;
import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.bytecode.analysis.types.RawJavaType;
import com.joutvhu.javadec.engine.bytecode.analysis.types.TypeConstants;
import com.joutvhu.javadec.engine.bytecode.analysis.types.discovery.InferredJavaType;

public class BadCastChainRewriter extends AbstractExpressionRewriter {
    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        expression = super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        if (expression instanceof CastExpression) {
            Expression child = ((CastExpression) expression).getChild();
            JavaTypeInstance type = expression.getInferredJavaType().getJavaTypeInstance().getDeGenerifiedType();
            JavaTypeInstance childType = child.getInferredJavaType().getJavaTypeInstance().getDeGenerifiedType();
            if (type.isComplexType() && childType.isComplexType()) {
                if (!childType.correctCanCastTo(type, null)) {
                    expression = new CastExpression(BytecodeLoc.NONE,
                            expression.getInferredJavaType(),
                        new CastExpression(BytecodeLoc.NONE, new InferredJavaType(TypeConstants.OBJECT, InferredJavaType.Source.UNKNOWN),
                                child, true)
                    );
                }
            } else if (childType == RawJavaType.BOOLEAN && child instanceof ConditionalExpression) {
                child = new TernaryExpression(BytecodeLoc.NONE, (ConditionalExpression)child, Literal.INT_ONE, Literal.INT_ZERO);
                expression = new CastExpression(BytecodeLoc.NONE,
                        expression.getInferredJavaType(), child
                        );
            }
        }
        return expression;
    }
}
