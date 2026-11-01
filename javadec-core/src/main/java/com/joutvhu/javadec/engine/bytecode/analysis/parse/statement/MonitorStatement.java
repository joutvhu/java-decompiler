package com.joutvhu.javadec.engine.bytecode.analysis.parse.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;

public abstract class MonitorStatement extends AbstractStatement {
    public MonitorStatement(BytecodeLoc loc) {
        super(loc);
    }
}
