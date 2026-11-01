package com.joutvhu.javadec.engine.bytecode.analysis.structured.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.BlockIdentifier;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.types.discovery.InferredJavaType;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.ConfusedCFRException;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.List;
import java.util.Vector;

public class UnstructuredCase extends AbstractUnStructuredStatement {
    private final List<Expression> values;
    private final BlockIdentifier blockIdentifier;
    private final InferredJavaType caseType;

    public UnstructuredCase(List<Expression> values, InferredJavaType caseType, BlockIdentifier blockIdentifier) {
        super(BytecodeLoc.NONE);
        this.values = values;
        this.caseType = caseType;
        this.blockIdentifier = blockIdentifier;
    }

    @Override
    public Dumper dump(Dumper dumper) {
        if (values.isEmpty()) {
            dumper.print("** default:").newln();
        } else {
            for (Expression value : values) {
                dumper.print("** case ").dump(value).print(":").newln();
            }
        }
        return dumper;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return getLoc();
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collectFrom(values);
        collector.collect(caseType.getJavaTypeInstance());
    }

    StructuredStatement getEmptyStructuredCase() {
        Op04StructuredStatement container = getContainer();
        return new StructuredCase(BytecodeLoc.TODO, values, caseType,
                new Op04StructuredStatement(
                        container.getIndex().justAfter(),
                        container.getBlockMembership(),
                        Block.getEmptyBlock(false)),
                blockIdentifier);
    }

    @Override
    public StructuredStatement claimBlock(Op04StructuredStatement innerBlock, BlockIdentifier blockIdentifier, Vector<BlockIdentifier> blocksCurrentlyIn) {
        if (blockIdentifier != this.blockIdentifier) {
            throw new ConfusedCFRException("Unstructured case being asked to claim wrong block. [" + blockIdentifier + " != " + this.blockIdentifier + "]");
        }
        return new StructuredCase(BytecodeLoc.TODO, values, caseType, innerBlock, blockIdentifier);
    }
}
