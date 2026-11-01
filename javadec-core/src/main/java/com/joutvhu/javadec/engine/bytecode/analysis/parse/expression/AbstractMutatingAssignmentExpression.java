package com.joutvhu.javadec.engine.bytecode.analysis.parse.expression;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.types.RawJavaType;
import com.joutvhu.javadec.engine.bytecode.analysis.types.discovery.InferredJavaType;
import com.joutvhu.javadec.engine.entities.exceptions.ExceptionCheck;

public abstract class AbstractMutatingAssignmentExpression extends AbstractAssignmentExpression {
    AbstractMutatingAssignmentExpression(BytecodeLoc loc, InferredJavaType inferredJavaType) {
        super(loc, inferredJavaType);
    }

    @Override
    public boolean canThrow(ExceptionCheck caught) {
        return !(getInferredJavaType().getJavaTypeInstance() instanceof RawJavaType);
    }

    @Override
    public boolean isValidStatement() {
        return true;
    }
}
