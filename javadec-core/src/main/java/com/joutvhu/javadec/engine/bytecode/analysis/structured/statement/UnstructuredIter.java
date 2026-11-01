package com.joutvhu.javadec.engine.bytecode.analysis.structured.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.LValue;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.BlockIdentifier;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.Vector;

public class UnstructuredIter extends AbstractUnStructuredStatement {
    private BlockIdentifier blockIdentifier;
    private LValue iterator;
    private Expression list;

    public UnstructuredIter(BytecodeLoc loc, BlockIdentifier blockIdentifier, LValue iterator, Expression list) {
        super(loc);
        this.blockIdentifier = blockIdentifier;
        this.iterator = iterator;
        this.list = list;
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.print("** for (").dump(iterator).print(" : ").dump(list).separator(")").newln();
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, list);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        iterator.collectTypeUsages(collector);
        collector.collectFrom(list);
    }

    @Override
    public StructuredStatement claimBlock(Op04StructuredStatement innerBlock, BlockIdentifier blockIdentifier, Vector<BlockIdentifier> blocksCurrentlyIn) {
        if (blockIdentifier != this.blockIdentifier) {
            throw new RuntimeException("ForIter statement claiming wrong block");
        }
        innerBlock.removeLastContinue(blockIdentifier);
        return new StructuredIter(getLoc(), blockIdentifier, iterator, list, innerBlock);
    }


}
