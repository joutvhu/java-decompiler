package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op3rewriters;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.StatementContainer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.SSAIdentifiers;

import java.util.Set;

public class NOPSearchingExpressionRewriter extends AbstractExpressionRewriter {

    private final Expression needle;
    private final Set<Expression> poison;
    private boolean found = false;
    private boolean poisoned = false;

    public NOPSearchingExpressionRewriter(Expression needle, Set<Expression> poison) {
        this.needle = needle;
        this.poison = poison;
    }

    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if (!found) {
            if (needle.equals(expression)) {
                found = true;
                return expression;
            }
        }
        if (poison.contains(expression)) {
            poisoned = true;
        }
        return expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
    }

    public boolean isFound() {
        return found && !poisoned;
    }
}
