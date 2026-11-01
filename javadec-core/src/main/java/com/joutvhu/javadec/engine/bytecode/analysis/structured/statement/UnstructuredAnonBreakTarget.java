package com.joutvhu.javadec.engine.bytecode.analysis.structured.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.BlockIdentifier;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.List;

public class UnstructuredAnonBreakTarget extends AbstractUnStructuredStatement {
    private BlockIdentifier blockIdentifier;

    public UnstructuredAnonBreakTarget(BlockIdentifier blockIdentifier) {
        super(BytecodeLoc.NONE);
        this.blockIdentifier = blockIdentifier;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return getLoc();
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper;
    }

    // Lose the comments.
    @Override
    public void linearizeInto(List<StructuredStatement> out) {
    }

    public BlockIdentifier getBlockIdentifier() {
        return blockIdentifier;
    }

    @Override
    public boolean isEffectivelyNOP() {
        return true;
    }
}
