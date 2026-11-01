package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.StatementContainer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.SSAIdentifiers;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.wildcard.WildcardMatch;
import com.joutvhu.javadec.engine.util.functors.NonaryFunction;

public class ExpressionWildcardReplacingRewriter extends AbstractExpressionRewriter {
    private final WildcardMatch wildcardMatch;
    private final Expression search;
    private final NonaryFunction<Expression> replacementFunction;

    ExpressionWildcardReplacingRewriter(WildcardMatch wildcardMatch, Expression search, NonaryFunction<Expression> replacementFunction) {
        this.wildcardMatch = wildcardMatch;
        this.search = search;
        this.replacementFunction = replacementFunction;
    }

    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if (expression == null) return null;
        if (search.equals(expression)) {
            Expression replacement = replacementFunction.invoke();
            if (replacement != null) {
                wildcardMatch.reset();
                return replacement;
            }
        }
        wildcardMatch.reset();
        return expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
    }

}
