package com.joutvhu.javadec.engine.bytecode.analysis.parse.expression;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.misc.Precedence;
import com.joutvhu.javadec.engine.bytecode.analysis.types.discovery.InferredJavaType;

public abstract class AbstractNewArray extends AbstractExpression {
    AbstractNewArray(BytecodeLoc loc, InferredJavaType inferredJavaType) {
        super(loc, inferredJavaType);
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.PAREN_SUB_MEMBER;
    }

    public abstract int getNumDims();

    public abstract int getNumSizedDims();

    public abstract Expression getDimSize(int dim);
}
