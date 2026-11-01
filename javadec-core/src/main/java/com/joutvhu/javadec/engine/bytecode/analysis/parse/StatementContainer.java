package com.joutvhu.javadec.engine.bytecode.analysis.parse;

import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.InstrIndex;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.BlockIdentifier;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.SSAIdentifiers;

import java.util.Set;

public interface StatementContainer<T> {
    T getStatement();

    T getTargetStatement(int idx);

    String getLabel();

    InstrIndex getIndex();

    void nopOut();

    void replaceStatement(T newTarget);

    void nopOutConditional();

    SSAIdentifiers<LValue> getSSAIdentifiers();

    Set<BlockIdentifier> getBlockIdentifiers();

    BlockIdentifier getBlockStarted();

    Set<BlockIdentifier> getBlocksEnded();

    void copyBlockInformationFrom(StatementContainer<T> other);

    void copyBytecodeInformationFrom(StatementContainer<T> other);
}
