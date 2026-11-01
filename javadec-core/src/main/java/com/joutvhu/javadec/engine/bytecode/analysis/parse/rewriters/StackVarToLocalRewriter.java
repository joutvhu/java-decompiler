package com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.LValue;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.StatementContainer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.ConditionalExpression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.LValueExpression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.StackValue;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.lvalue.LocalVariable;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.lvalue.StackSSALabel;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.SSAIdentifiers;
import com.joutvhu.javadec.engine.util.collections.MapFactory;

import java.util.Map;

public class StackVarToLocalRewriter implements ExpressionRewriter {

    private final Map<StackSSALabel, LocalVariable> replacements = MapFactory.newMap();
    private int idx = 0;

    public void handleStatement(StatementContainer statementContainer) {

    }

    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if (expression instanceof StackValue) {
            // We rewrite as an LValue expression.
            StackValue stackValue = (StackValue) expression;
            return new LValueExpression(getReplacement(stackValue.getStackValue()));
        }
        return expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
    }

    @Override
    public ConditionalExpression rewriteExpression(ConditionalExpression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        Expression res = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
        return (ConditionalExpression) res;
    }

//    @Override
//    public AbstractAssignmentExpression rewriteExpression(AbstractAssignmentExpression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
//        Expression res = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
//        return (AbstractAssignmentExpression) res;
//    }

    @Override
    public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if (lValue instanceof StackSSALabel) {
            return getReplacement((StackSSALabel) lValue);
        }
        return lValue.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
    }

    @Override
    public StackSSALabel rewriteExpression(StackSSALabel lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        // If we get here we're in trouble....  Something's explicitly holding onto an SSALabel?
        throw new UnsupportedOperationException();
    }

    private LocalVariable getReplacement(StackSSALabel stackSSALabel) {
        LocalVariable res = replacements.get(stackSSALabel);
        if (res != null) return res;
        res = new LocalVariable("v" + idx++, stackSSALabel.getInferredJavaType());
        replacements.put(stackSSALabel, res);
        return res;
    }
}
