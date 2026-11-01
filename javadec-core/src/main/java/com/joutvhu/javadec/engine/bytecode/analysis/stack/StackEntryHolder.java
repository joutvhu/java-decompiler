package com.joutvhu.javadec.engine.bytecode.analysis.stack;

import com.joutvhu.javadec.engine.bytecode.analysis.types.StackType;
import com.joutvhu.javadec.engine.util.DecompilerComment;

import java.util.Set;

public class StackEntryHolder {
    private StackEntry stackEntry;

    StackEntryHolder(StackType stackType) {
        stackEntry = new StackEntry(stackType);
    }

    public void mergeWith(StackEntryHolder other, Set<DecompilerComment> comments) {
        stackEntry.mergeWith(other.stackEntry, comments);
        other.stackEntry = stackEntry;
    }

    @Override
    public String toString() {
        return stackEntry.toString();
    }

    public StackEntry getStackEntry() {
        return stackEntry;
    }
}
