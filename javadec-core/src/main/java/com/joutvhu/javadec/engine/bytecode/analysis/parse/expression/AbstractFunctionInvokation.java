package com.joutvhu.javadec.engine.bytecode.analysis.parse.expression;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.StatementContainer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.SSAIdentifiers;
import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.bytecode.analysis.types.MethodPrototype;
import com.joutvhu.javadec.engine.bytecode.analysis.types.discovery.InferredJavaType;
import com.joutvhu.javadec.engine.entities.constantpool.ConstantPoolEntryMethodRef;

import java.util.List;

public abstract class AbstractFunctionInvokation extends AbstractExpression {
    private final ConstantPoolEntryMethodRef function;
    private final MethodPrototype methodPrototype;

    AbstractFunctionInvokation(BytecodeLoc loc, ConstantPoolEntryMethodRef function, InferredJavaType inferredJavaType) {
        super(loc, inferredJavaType);
        this.function = function;
        this.methodPrototype = function.getMethodPrototype();
    }

    public abstract void applyExpressionRewriterToArgs(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags);

    public abstract void setExplicitGenerics(List<JavaTypeInstance> types);

    public abstract List<JavaTypeInstance> getExplicitGenerics();

    public ConstantPoolEntryMethodRef getFunction() {
        return function;
    }

    public MethodPrototype getMethodPrototype() { return methodPrototype; }

    public String getName() {
        return methodPrototype.getName();
    }

    String getFixedName() {
        return methodPrototype.getFixedName();
    }

    @Override
    public boolean isValidStatement() {
        return true;
    }

    public abstract List<Expression> getArgs();
}
