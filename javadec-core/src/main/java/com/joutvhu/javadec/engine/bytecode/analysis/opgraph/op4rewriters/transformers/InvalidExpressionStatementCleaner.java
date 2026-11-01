package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.transformers;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredScope;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.StructuredAssignment;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.variables.VariableFactory;

import java.util.Set;

public class InvalidExpressionStatementCleaner extends AbstractExpressionRewriter implements StructuredStatementTransformer {

    private VariableFactory variableFactory;

    public InvalidExpressionStatementCleaner(VariableFactory variableNamer) {
        this.variableFactory = variableNamer;
    }

    public void transform(Op04StructuredStatement root) {
        StructuredScope structuredScope = new StructuredScope();
        root.transform(this, structuredScope);
    }

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        in.transformStructuredChildren(this, scope);
        if (in instanceof StructuredExpressionStatement) {
            Expression exp = ((StructuredExpressionStatement) in).getExpression();
            if (!exp.isValidStatement()) {
                /* Have to assign to an ignored temporary, or discard.
                 * We prefer not to discard, as that involves detecting side effects
                 * and hides bytecode.
                 */
                return new StructuredAssignment(BytecodeLoc.TODO, variableFactory.ignoredVariable(exp.getInferredJavaType()), exp, true);
            }
        }
        return in;
    }
}
