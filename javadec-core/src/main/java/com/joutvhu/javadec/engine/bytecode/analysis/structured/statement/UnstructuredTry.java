package com.joutvhu.javadec.engine.bytecode.analysis.structured.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.BlockIdentifier;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.entities.exceptions.ExceptionGroup;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.Vector;

public class UnstructuredTry extends AbstractUnStructuredStatement {
    private final ExceptionGroup exceptionGroup;

    public UnstructuredTry(ExceptionGroup exceptionGroup) {
        super(BytecodeLoc.NONE);
        this.exceptionGroup = exceptionGroup;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return getLoc();
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.print("** try " + exceptionGroup + " { ").newln();
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
    }

    StructuredStatement getEmptyTry() {
        return new StructuredTry(new Op04StructuredStatement(Block.getEmptyBlock(true)), exceptionGroup.getTryBlockIdentifier());
    }

    @Override
    public StructuredStatement claimBlock(Op04StructuredStatement innerBlock, BlockIdentifier blockIdentifier, Vector<BlockIdentifier> blocksCurrentlyIn) {
        if (blockIdentifier == exceptionGroup.getTryBlockIdentifier()) {
            return new StructuredTry(innerBlock, blockIdentifier);
        } else {
            return null;
        }
    }
}
