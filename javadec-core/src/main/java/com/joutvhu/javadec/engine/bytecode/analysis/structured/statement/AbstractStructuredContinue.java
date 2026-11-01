package com.joutvhu.javadec.engine.bytecode.analysis.structured.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.BlockIdentifier;

public abstract class AbstractStructuredContinue extends AbstractStructuredStatement {
    public AbstractStructuredContinue(BytecodeLoc loc) {
        super(loc);
    }

    public abstract BlockIdentifier getContinueTgt();
}
