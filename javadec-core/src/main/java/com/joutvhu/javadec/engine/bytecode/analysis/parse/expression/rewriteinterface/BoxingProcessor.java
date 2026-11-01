package com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.rewriteinterface;

import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.PrimitiveBoxingRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.StatementContainer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.SSAIdentifiers;

public interface BoxingProcessor {
    // return true if boxing finished.
    boolean rewriteBoxing(PrimitiveBoxingRewriter boxingRewriter);

    void applyNonArgExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags);
}
