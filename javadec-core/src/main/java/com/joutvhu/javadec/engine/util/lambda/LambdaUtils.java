package com.joutvhu.javadec.engine.util.lambda;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.Literal;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.literal.TypedLiteral;
import com.joutvhu.javadec.engine.bytecode.analysis.types.MethodPrototype;
import com.joutvhu.javadec.engine.entities.Method;
import com.joutvhu.javadec.engine.entities.constantpool.*;
import com.joutvhu.javadec.engine.util.ConfusedCFRException;

public class LambdaUtils {

    private static TypedLiteral getTypedLiteral(Expression e) {
        if (!(e instanceof Literal)) throw new IllegalArgumentException("Expecting literal");
        return ((Literal) e).getValue();
    }

    private static TypedLiteral.LiteralType getLiteralType(Expression e) {
        TypedLiteral t = getTypedLiteral(e);
        return t.getType();
    }

    public static ConstantPoolEntryMethodHandle getHandle(Expression e) {
        TypedLiteral t = getTypedLiteral(e);
        if (t.getType() != TypedLiteral.LiteralType.MethodHandle) {
            throw new IllegalArgumentException("Expecting method handle");
        }
        return (ConstantPoolEntryMethodHandle) t.getValue();
    }

    private static ConstantPoolEntryMethodType getType(Expression e) {
        TypedLiteral t = getTypedLiteral(e);
        if (t.getType() != TypedLiteral.LiteralType.MethodType) {
            throw new IllegalArgumentException("Expecting method type");
        }
        return (ConstantPoolEntryMethodType) t.getValue();
    }

    public static MethodPrototype getLiteralProto(Expression arg) {
        TypedLiteral.LiteralType flavour = getLiteralType(arg);

        switch (flavour) {
            case MethodHandle: {
                ConstantPoolEntryMethodHandle targetFnHandle = getHandle(arg);
                ConstantPoolEntryMethodRef targetMethRef = targetFnHandle.getMethodRef();
                return targetMethRef.getMethodPrototype();
            }
            case MethodType: {
                ConstantPoolEntryMethodType targetFnType = getType(arg);
                ConstantPoolEntryUTF8 descriptor = targetFnType.getDescriptor();
                return ConstantPoolUtils.parseJavaMethodPrototype(null,null, null, null, false, Method.MethodConstructor.NOT, descriptor, targetFnType.getCp(), false, false, null, descriptor.getValue());
            }
            default:
                throw new ConfusedCFRException("Can't understand this lambda - disable lambdas.");
        }
    }

}
