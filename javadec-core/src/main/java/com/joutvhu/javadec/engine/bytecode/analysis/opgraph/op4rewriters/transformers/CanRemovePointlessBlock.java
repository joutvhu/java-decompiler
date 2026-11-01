package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.transformers;

import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredScope;

public interface CanRemovePointlessBlock {
    void removePointlessBlocks(StructuredScope scope);
}
