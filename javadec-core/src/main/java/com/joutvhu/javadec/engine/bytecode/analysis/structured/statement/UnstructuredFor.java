package com.joutvhu.javadec.engine.bytecode.analysis.structured.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.AbstractAssignmentExpression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.ConditionalExpression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.statement.AssignmentSimple;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.BlockIdentifier;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.StringUtils;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.List;
import java.util.Vector;

public class UnstructuredFor extends AbstractUnStructuredStatement {
    private ConditionalExpression condition;
    private BlockIdentifier blockIdentifier;
    private AssignmentSimple initial;
    private List<AbstractAssignmentExpression> assignments;

    public UnstructuredFor(BytecodeLoc loc, ConditionalExpression condition, BlockIdentifier blockIdentifier, AssignmentSimple initial, List<AbstractAssignmentExpression> assignments) {
        super(loc);
        this.condition = condition;
        this.blockIdentifier = blockIdentifier;
        this.initial = initial;
        this.assignments = assignments;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, assignments, condition, initial);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collectFrom(condition);
        collector.collectFrom(assignments);
        // collector.collectFrom(initial);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        dumper.print("** for (").dump(initial).print("; ").dump(condition).print("; ");
        boolean first = true;
        for (AbstractAssignmentExpression assignment : assignments) {
            first = StringUtils.comma(first, dumper);
            dumper.dump(assignment);
        }
        return dumper.separator(")").newln();
    }

    @Override
    public StructuredStatement claimBlock(Op04StructuredStatement innerBlock, BlockIdentifier blockIdentifier, Vector<BlockIdentifier> blocksCurrentlyIn) {
        if (blockIdentifier != this.blockIdentifier) {
            throw new RuntimeException("For statement claiming wrong block");
        }
        innerBlock.removeLastContinue(blockIdentifier);
        return new StructuredFor(getLoc(), condition, initial, assignments, innerBlock, blockIdentifier);
    }

}
