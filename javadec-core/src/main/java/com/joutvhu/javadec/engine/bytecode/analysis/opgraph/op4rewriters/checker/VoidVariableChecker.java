package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.checker;

import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredScope;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.StructuredDefinition;
import com.joutvhu.javadec.engine.bytecode.analysis.types.RawJavaType;
import com.joutvhu.javadec.engine.bytecode.analysis.types.discovery.InferredJavaType;
import com.joutvhu.javadec.engine.util.DecompilerComment;
import com.joutvhu.javadec.engine.util.DecompilerComments;

public class VoidVariableChecker implements Op04Checker {
    private boolean found = false;

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        if (found) return in;
        if (in instanceof StructuredDefinition) {
            InferredJavaType inferredJavaType = ((StructuredDefinition) in).getLvalue().getInferredJavaType();
            if (inferredJavaType != null && inferredJavaType.getJavaTypeInstance().getRawTypeOfSimpleType() == RawJavaType.VOID) {
                found = true;
                return in;
            }
        }

        in.transformStructuredChildren(this, scope);
        return in;
    }

    @Override
    public void commentInto(DecompilerComments comments) {
        if (found) {
            comments.addComment(DecompilerComment.VOID_DECLARATION);
        }
    }
}
