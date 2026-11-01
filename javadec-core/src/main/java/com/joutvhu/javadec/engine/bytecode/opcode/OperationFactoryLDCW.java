package com.joutvhu.javadec.engine.bytecode.opcode;

import com.joutvhu.javadec.engine.bytecode.analysis.stack.StackDelta;
import com.joutvhu.javadec.engine.bytecode.analysis.stack.StackDeltaImpl;
import com.joutvhu.javadec.engine.bytecode.analysis.stack.StackSim;
import com.joutvhu.javadec.engine.bytecode.analysis.types.StackType;
import com.joutvhu.javadec.engine.bytecode.analysis.types.StackTypes;
import com.joutvhu.javadec.engine.entities.constantpool.ConstantPoolEntry;
import com.joutvhu.javadec.engine.entities.Method;
import com.joutvhu.javadec.engine.util.ConfusedCFRException;

public class OperationFactoryLDCW extends OperationFactoryCPEntryW {

    @Override
    public StackDelta getStackDelta(JVMInstr instr, byte[] data, ConstantPoolEntry[] cpEntries,
                                    StackSim stackSim, Method method) {
        StackType stackType = OperationFactoryLDC.getStackType(cpEntries[0]);
        int requiredComputationCategory = getRequiredComputationCategory();
        if (stackType.getComputationCategory() != requiredComputationCategory) {
            throw new ConfusedCFRException("Got a literal, but expected a different category");
        }

        return new StackDeltaImpl(StackTypes.EMPTY, stackType.asList());
    }

    protected int getRequiredComputationCategory() {
        return 1;
    }

}
