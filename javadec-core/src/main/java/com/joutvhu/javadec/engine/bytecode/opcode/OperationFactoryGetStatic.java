package com.joutvhu.javadec.engine.bytecode.opcode;

import com.joutvhu.javadec.engine.bytecode.analysis.stack.StackDelta;
import com.joutvhu.javadec.engine.bytecode.analysis.stack.StackDeltaImpl;
import com.joutvhu.javadec.engine.bytecode.analysis.stack.StackSim;
import com.joutvhu.javadec.engine.bytecode.analysis.types.StackType;
import com.joutvhu.javadec.engine.bytecode.analysis.types.StackTypes;
import com.joutvhu.javadec.engine.entities.constantpool.ConstantPoolEntry;
import com.joutvhu.javadec.engine.entities.constantpool.ConstantPoolEntryFieldRef;
import com.joutvhu.javadec.engine.entities.Method;
import com.joutvhu.javadec.engine.util.ConfusedCFRException;

public class OperationFactoryGetStatic extends OperationFactoryCPEntryW {

    @Override
    public StackDelta getStackDelta(JVMInstr instr, byte[] data, ConstantPoolEntry[] cpEntries,
                                    StackSim stackSim, Method method) {
        ConstantPoolEntryFieldRef fieldRef = (ConstantPoolEntryFieldRef) cpEntries[0];
        if (fieldRef == null) throw new ConfusedCFRException("Expecting fieldRef");
        StackType stackType = fieldRef.getStackType();
        return new StackDeltaImpl(StackTypes.EMPTY, stackType.asList());
    }

}
