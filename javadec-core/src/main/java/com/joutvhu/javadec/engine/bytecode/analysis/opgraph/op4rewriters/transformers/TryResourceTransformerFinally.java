package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.transformers;

import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredScope;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.StructuredTry;
import com.joutvhu.javadec.engine.entities.ClassFile;

public abstract class TryResourceTransformerFinally extends TryResourcesTransformerBase{
    public TryResourceTransformerFinally(ClassFile classFile) {
        super(classFile);
    }

    @Override
    protected ResourceMatch getResourceMatch(StructuredTry structuredTry, StructuredScope scope) {
        Op04StructuredStatement finallyBlock = structuredTry.getFinallyBlock();
        return findResourceFinally(finallyBlock);
    }

    // If the finally block is
    // if (autoclosable != null) {
    //    close(exception, autoclosable)
    // }
    //
    // or
    //
    // close(exception, autoclosable)
    //
    // we can lift the autocloseable into the try.
    protected abstract ResourceMatch findResourceFinally(Op04StructuredStatement finallyBlock);

}
