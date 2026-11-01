package com.joutvhu.javadec.engine.bytecode.analysis.parse.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.CloneHelper;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.DeepCloneable;

public abstract class ReturnStatement extends AbstractStatement {

    public ReturnStatement(BytecodeLoc loc) {
        super(loc);
    }

    @Override
    public boolean fallsToNext() {
        return false;
    }

    @Override
    public ReturnStatement outerDeepClone(CloneHelper cloneHelper) {
        throw new UnsupportedOperationException();
    }
}
