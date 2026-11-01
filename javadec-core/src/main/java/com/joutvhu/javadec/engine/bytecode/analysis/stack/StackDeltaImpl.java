package com.joutvhu.javadec.engine.bytecode.analysis.stack;

import com.joutvhu.javadec.engine.bytecode.analysis.types.StackTypes;
import com.joutvhu.javadec.engine.util.ConfusedCFRException;

public class StackDeltaImpl implements StackDelta {
    private final StackTypes consumed;
    private final StackTypes produced;

    public StackDeltaImpl(StackTypes consumed, StackTypes produced) {
        if (consumed == null || produced == null) {
            throw new ConfusedCFRException("Must not have null stackTypes");
        }
        this.consumed = consumed;
        this.produced = produced;
    }

    @Override
    public boolean isNoOp() {
        return consumed.isEmpty() && produced.isEmpty();
    }

    @Override
    public StackTypes getConsumed() {
        return consumed;
    }

    @Override
    public StackTypes getProduced() {
        return produced;
    }

    @Override
    public long getChange() {
        return produced.size() - consumed.size();
    }

    @Override
    public String toString() {
        return "Consumes " + consumed + ", Produces " + produced;
    }
}
