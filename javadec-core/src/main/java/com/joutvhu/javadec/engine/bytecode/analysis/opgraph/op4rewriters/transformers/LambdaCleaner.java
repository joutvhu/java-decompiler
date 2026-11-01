package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.transformers;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.StatementContainer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.LambdaExpression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.Pair;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.SSAIdentifiers;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredScope;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.expression.StructuredStatementExpression;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.Block;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.StructuredReturn;

import java.util.LinkedList;

public class LambdaCleaner extends AbstractExpressionRewriter implements StructuredStatementTransformer {

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

    private static LambdaExpression rebuildLambda(LambdaExpression e, Expression body) {
        return new LambdaExpression(BytecodeLoc.TODO, e.getInferredJavaType(), e.getArgs(), e.explicitArgTypes(), body);
    }

    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if (expression instanceof LambdaExpression) {
            LambdaExpression lambda = (LambdaExpression)expression;
            Expression result = lambda.getResult();
            if (result instanceof StructuredStatementExpression) {
                StructuredStatementExpression structuredStatementExpression = (StructuredStatementExpression)result;
                StructuredStatement content = structuredStatementExpression.getContent();
                if (content instanceof Block) {
                    Block block = (Block)content;
                    Pair<Boolean, Op04StructuredStatement> singleStatement = block.getOneStatementIfPresent();
                    if (singleStatement.getSecond() != null) {
                        StructuredStatement statement = singleStatement.getSecond().getStatement();
                        if (statement instanceof StructuredReturn) {
                            expression = rebuildLambda(lambda, ((StructuredReturn)statement).getValue());
                        } else if (statement instanceof StructuredExpressionStatement) {
                            expression = rebuildLambda(lambda, ((StructuredExpressionStatement) statement).getExpression());
                        }
                    } else {
                        if (singleStatement.getFirst()) {
                            Expression empty = new StructuredStatementExpression(expression.getInferredJavaType(), new Block(new LinkedList<Op04StructuredStatement>(), true));
                            expression = rebuildLambda(lambda, empty);
                        }
                    }
                }
            }
        }
        return expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
    }
}
