package com.joutvhu.javadec.engine.bytecode.analysis.parse.expression;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.LValue;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.misc.Precedence;

import java.util.Set;

public interface ConditionalExpression extends Expression {
    ConditionalExpression getNegated();

    int getSize(Precedence outerPrecedence);

    ConditionalExpression getDemorganApplied(boolean amNegating);

    /*
     * Normalise tree layout so ((a || b) || c) --> (a || (b || c)).
     * This is useful so any patterns can know what they're matching against.
     */
    ConditionalExpression getRightDeep();

    Set<LValue> getLoopLValues();

    ConditionalExpression optimiseForType();

    ConditionalExpression simplify();
}
