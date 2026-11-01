package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters;

import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.transformers.ExpressionRewriterTransformer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.StatementContainer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.MethodHandlePlaceholder;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.SSAIdentifiers;
import com.joutvhu.javadec.engine.entities.ClassFile;
import com.joutvhu.javadec.engine.entities.FakeMethod;
import com.joutvhu.javadec.engine.entities.Method;
import com.joutvhu.javadec.engine.state.TypeUsageCollectingDumper;

public class FakeMethodRewriter {
    public static void rewrite(ClassFile classFile, TypeUsageCollectingDumper typeUsage) {
        ExpressionRewriterTransformer trans = new ExpressionRewriterTransformer(new Rewriter(classFile, typeUsage));
        for (Method method : classFile.getMethods()) {
            if (method.hasCodeAttribute()) {
                Op04StructuredStatement code = method.getAnalysis();
                trans.transform(code);
            }
        }
    }

    private static class Rewriter extends AbstractExpressionRewriter {
        private final ClassFile classFile;
        private TypeUsageCollectingDumper typeUsage;

        Rewriter(ClassFile classFile, TypeUsageCollectingDumper typeUsage) {
            this.classFile = classFile;
            this.typeUsage = typeUsage;
        }

        @Override
        public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            if (expression instanceof MethodHandlePlaceholder) {
                FakeMethod method = ((MethodHandlePlaceholder) expression).addFakeMethod(classFile);
                typeUsage.dump(method);
            }
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }
    }
}
