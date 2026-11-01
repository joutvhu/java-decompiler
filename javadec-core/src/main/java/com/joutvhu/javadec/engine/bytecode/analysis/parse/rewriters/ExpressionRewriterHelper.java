package com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.StatementContainer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.SSAIdentifiers;

import java.util.List;

public class ExpressionRewriterHelper {
    public static void applyForwards(List<Expression> list, ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        for (int x = 0; x < list.size(); ++x) {
            list.set(x, expressionRewriter.rewriteExpression(list.get(x), ssaIdentifiers, statementContainer, flags));
        }
    }

    public static void applyBackwards(List<Expression> list, ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        for (int x = list.size()-1; x >= 0; --x) {
            list.set(x, expressionRewriter.rewriteExpression(list.get(x), ssaIdentifiers, statementContainer, flags));
        }
    }
}
