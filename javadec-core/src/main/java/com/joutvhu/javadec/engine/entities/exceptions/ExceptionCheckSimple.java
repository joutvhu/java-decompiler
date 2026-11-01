package com.joutvhu.javadec.engine.entities.exceptions;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation;
import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;

import java.util.Set;

public class ExceptionCheckSimple implements ExceptionCheck {
    public static final ExceptionCheck INSTANCE = new ExceptionCheckSimple();

    private ExceptionCheckSimple() {
    }

    @Override
    public boolean checkAgainst(Set<? extends JavaTypeInstance> thrown) {
        return true;
    }

    @Override
    public boolean checkAgainst(AbstractMemberFunctionInvokation functionInvokation) {
        return true;
    }

    @Override
    public boolean checkAgainstException(Expression expression) {
        return true;
    }

    @Override
    public boolean mightCatchUnchecked() {
        return true;
    }
}
