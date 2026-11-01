package com.joutvhu.javadec.engine.bytecode.analysis.structured.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.output.Dumper;

public class UnstructuredGoto extends AbstractUnStructuredStatement {

    public UnstructuredGoto(BytecodeLoc loc) {
        super(loc);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return getLoc();
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.print("** GOTO " + getContainer().getTargetLabel(0)).newln();
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
    }
}
