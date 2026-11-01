package com.joutvhu.javadec.engine.bytecode.analysis.structured.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.BlockIdentifier;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.Vector;

public class UnstructuredSynchronized extends AbstractUnStructuredStatement {
    private Expression monitor;
    private BlockIdentifier blockIdentifier;

    public UnstructuredSynchronized(BytecodeLoc loc, Expression monitor, BlockIdentifier blockIdentifier) {
        super(loc);
        this.monitor = monitor;
        this.blockIdentifier = blockIdentifier;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, monitor);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        monitor.collectTypeUsages(collector);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.print("** synchronized (").dump(monitor).separator(")").newln();
    }

    @Override
    public StructuredStatement claimBlock(Op04StructuredStatement innerBlock, BlockIdentifier blockIdentifier, Vector<BlockIdentifier> blocksCurrentlyIn) {
        if (blockIdentifier != this.blockIdentifier) {
            throw new RuntimeException("MONITOREXIT statement claiming wrong block");
        }

        return new StructuredSynchronized(getLoc(), monitor, innerBlock);
    }

}
