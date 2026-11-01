package com.joutvhu.javadec.engine.entities.constantpool;

import com.joutvhu.javadec.engine.bytecode.analysis.types.StackType;

public interface ConstantPoolEntryLiteral {
    StackType getStackType();
}
