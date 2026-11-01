package com.joutvhu.javadec.engine.bytecode.analysis.structured.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.BlockIdentifier;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.ConfusedCFRException;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.List;
import java.util.Vector;

public class UnstructuredSwitch extends AbstractUnStructuredStatement {
    private Expression switchOn;
    private final BlockIdentifier blockIdentifier;
    private boolean safeExpression;

    public UnstructuredSwitch(BytecodeLoc loc, Expression switchOn, BlockIdentifier blockIdentifier, boolean safeExpression) {
        super(loc);
        this.switchOn = switchOn;
        this.blockIdentifier = blockIdentifier;
        this.safeExpression = safeExpression;
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.print("** switch (").dump(switchOn).separator(")").newln();
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, switchOn);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collectFrom(switchOn);
    }

    @Override
    public StructuredStatement claimBlock(Op04StructuredStatement innerBlock, BlockIdentifier blockIdentifier, Vector<BlockIdentifier> blocksCurrentlyIn) {
        if (blockIdentifier != this.blockIdentifier) {
            throw new ConfusedCFRException("Unstructured switch being asked to claim wrong block. [" + blockIdentifier + " != " + this.blockIdentifier + "]");
        }
        /*
         * If the last statement is an unstructured case, then we've got a case with no body.  Transform it into a structured
         * case.
         */
        if (innerBlock.getStatement() instanceof Block) {
            Block block = (Block) innerBlock.getStatement();
            List<Op04StructuredStatement> statements = block.getBlockStatements();
            Op04StructuredStatement last = statements.get(statements.size() - 1);
            if (last.getStatement() instanceof UnstructuredCase) {
                UnstructuredCase caseStatement = (UnstructuredCase) (last.getStatement());
                last.replaceStatement(caseStatement.getEmptyStructuredCase());
            }
        }
        return new StructuredSwitch(getLoc(), switchOn, innerBlock, blockIdentifier, safeExpression);
    }
}
