package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.util;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.LValueExpression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.MemberFunctionInvokation;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.wildcard.WildcardMatch;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.StructuredComment;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.types.MethodPrototype;
import com.joutvhu.javadec.engine.entities.Method;

import java.util.List;

public class ConstructorUtils {
    public static MethodPrototype getDelegatingPrototype(Method constructor) {
        List<Op04StructuredStatement> statements = MiscStatementTools.getBlockStatements(constructor.getAnalysis());
        if (statements == null) return null;
        for (Op04StructuredStatement statement : statements) {
            StructuredStatement structuredStatement = statement.getStatement();
            if (structuredStatement instanceof StructuredComment) continue;
            if (!(structuredStatement instanceof StructuredExpressionStatement)) return null;
            StructuredExpressionStatement structuredExpressionStatement = (StructuredExpressionStatement) structuredStatement;

            WildcardMatch wcm1 = new WildcardMatch();
            StructuredStatement test = new StructuredExpressionStatement(BytecodeLoc.NONE, wcm1.getMemberFunction("m", null, true /* this method */, new LValueExpression(wcm1.getLValueWildCard("o")), null), false);
            if (test.equals(structuredExpressionStatement)) {
                MemberFunctionInvokation m = wcm1.getMemberFunction("m").getMatch();
                MethodPrototype prototype = m.getMethodPrototype();
                return prototype;
            }
            return null;
        }
        return null;
    }

    public static boolean isDelegating(Method constructor) {
        return getDelegatingPrototype(constructor) != null;
    }
}

