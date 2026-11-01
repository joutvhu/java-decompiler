package com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.ArithmeticOperation;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.Literal;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.TernaryExpression;

public interface ExpressionVisitor<T> {
    T visit(Expression e);

    T visit(Literal l);

    T visit(TernaryExpression e);

    T visit(ArithmeticOperation e);
}
