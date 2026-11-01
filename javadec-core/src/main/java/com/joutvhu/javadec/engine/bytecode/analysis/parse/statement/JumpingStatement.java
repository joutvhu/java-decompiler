package com.joutvhu.javadec.engine.bytecode.analysis.parse.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Statement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.JumpType;

public abstract class JumpingStatement extends AbstractStatement {
    public JumpingStatement(BytecodeLoc loc) {
        super(loc);
    }

    public abstract Statement getJumpTarget();

    public abstract JumpType getJumpType();

    public abstract void setJumpType(JumpType jumpType);

    public abstract boolean isConditional();
}
