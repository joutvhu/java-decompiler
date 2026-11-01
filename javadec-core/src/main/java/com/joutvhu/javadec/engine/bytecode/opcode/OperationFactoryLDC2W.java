package com.joutvhu.javadec.engine.bytecode.opcode;

public class OperationFactoryLDC2W extends OperationFactoryLDCW {
    @Override
    protected int getRequiredComputationCategory() {
        return 2;
    }
}
