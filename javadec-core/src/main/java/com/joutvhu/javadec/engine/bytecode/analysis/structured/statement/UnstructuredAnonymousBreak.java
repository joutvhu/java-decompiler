package com.joutvhu.javadec.engine.bytecode.analysis.structured.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.BlockIdentifier;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.Vector;

public class UnstructuredAnonymousBreak extends AbstractUnStructuredStatement {

    private final BlockIdentifier blockEnding;

    public UnstructuredAnonymousBreak(BytecodeLoc loc, BlockIdentifier blockEnding) {
        super(loc);
        this.blockEnding = blockEnding;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return getLoc();
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.print("** break ").print(blockEnding.getName()).newln();
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
    }

    @Override
    public StructuredStatement informBlockHeirachy(Vector<BlockIdentifier> blockIdentifiers) {
        return null;
    }

    StructuredStatement tryExplicitlyPlaceInBlock(BlockIdentifier block) {
        if (block != blockEnding) {
            return this;
        }
        block.addForeignRef();
        return new StructuredBreak(getLoc(), block, false);
    }
}
