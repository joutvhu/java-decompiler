package com.joutvhu.javadec.engine.entities.exceptions;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation;
import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;

import java.util.Set;

public interface ExceptionCheck {
    boolean checkAgainst(Set<? extends JavaTypeInstance> thrown);

    // Might this throw in a way which means it can't be moved into the exception block?
    boolean checkAgainst(AbstractMemberFunctionInvokation functionInvokation);

    boolean checkAgainstException(Expression expression);

    boolean mightCatchUnchecked();
}
