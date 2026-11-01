package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.LValue;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.StatementContainer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.SSAIdentifiers;

import java.util.Map;

public class LValueReplacingRewriter extends AbstractExpressionRewriter {
    private final Map<LValue, LValue> replacements;

    public LValueReplacingRewriter(Map<LValue, LValue> replacements) {
        this.replacements = replacements;
    }

    @Override
    public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        LValue replacement = replacements.get(lValue);
        if (replacement != null) {
            return replacement;
        }
        return lValue.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
    }

}
