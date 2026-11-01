package com.joutvhu.javadec.engine.bytecode.analysis.parse.expression;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.misc.Precedence;
import com.joutvhu.javadec.engine.util.ConfusedCFRException;

public enum BoolOp {
    OR("||", Precedence.LOG_OR),
    AND("&&", Precedence.LOG_AND);

    private final String showAs;
    private final Precedence precedence;

    BoolOp(String showAs, Precedence precedence) {
        this.showAs = showAs;
        this.precedence = precedence;
    }

    public String getShowAs() {
        return showAs;
    }

    public Precedence getPrecedence() {
        return precedence;
    }

    public BoolOp getDemorgan() {
        switch (this) {
            case OR:
                return AND;
            case AND:
                return OR;
            default:
                throw new ConfusedCFRException("Unknown op.");
        }
    }
}
