package com.joutvhu.javadec.engine.bytecode.analysis.structured.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.BlockIdentifier;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.Set;
import java.util.Vector;

public class UnstructuredBreak extends AbstractUnStructuredStatement {

    private final Set<BlockIdentifier> blocksEnding;

    public UnstructuredBreak(BytecodeLoc loc, Set<BlockIdentifier> blocksEnding) {
        super(loc);
        this.blocksEnding = blocksEnding;
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.print("** break;").newln();
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return getLoc();
    }

    @Override
    public StructuredStatement informBlockHeirachy(Vector<BlockIdentifier> blockIdentifiers) {
        /*
         * Find which of blocksEnding is outermost ( earliest ).
         */
        int index = Integer.MAX_VALUE;
        BlockIdentifier bestBlock = null;
        for (BlockIdentifier block : blocksEnding) {
            int posn = blockIdentifiers.indexOf(block);
            if (posn >= 0 && index > posn) {
                index = posn;
                bestBlock = block;
            }
        }
        if (bestBlock == null) {
//            System.out.println("Unstructured break doesn't know best block out of " + blocksEnding);
            return null;
        }
        boolean localBreak = false;
        BlockIdentifier outermostBreakable = BlockIdentifier.getInnermostBreakable(blockIdentifiers);
        if (outermostBreakable == bestBlock) {
            localBreak = true;
        } else {
            bestBlock.addForeignRef();
        }
        return new StructuredBreak(getLoc(), bestBlock, localBreak);
    }
}
