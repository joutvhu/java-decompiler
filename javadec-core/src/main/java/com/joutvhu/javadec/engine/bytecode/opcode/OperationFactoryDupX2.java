package com.joutvhu.javadec.engine.bytecode.opcode;

import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op01WithProcessedDataAndByteJumps;
import com.joutvhu.javadec.engine.bytecode.analysis.stack.StackDelta;
import com.joutvhu.javadec.engine.bytecode.analysis.stack.StackDeltaImpl;
import com.joutvhu.javadec.engine.bytecode.analysis.stack.StackSim;
import com.joutvhu.javadec.engine.entities.constantpool.ConstantPool;
import com.joutvhu.javadec.engine.entities.constantpool.ConstantPoolEntry;
import com.joutvhu.javadec.engine.entities.Method;
import com.joutvhu.javadec.engine.util.bytestream.ByteData;

public class OperationFactoryDupX2 extends OperationFactoryDupBase {

    @Override
    public StackDelta getStackDelta(JVMInstr instr, byte[] data, ConstantPoolEntry[] cpEntries,
                                    StackSim stackSim, Method method) {
        if (getCat(stackSim, 1) == 2) {
            checkCat(stackSim, 0, 1);
            return new StackDeltaImpl(
                    getStackTypes(stackSim, 0, 1),
                    getStackTypes(stackSim, 0, 1, 0)
            );
        } else {
            checkCat(stackSim, 0, 1);
            checkCat(stackSim, 2, 1);
            return new StackDeltaImpl(
                    getStackTypes(stackSim, 0, 1, 2),
                    getStackTypes(stackSim, 0, 1, 2, 0)
            );
        }
    }

    @Override
    public Op01WithProcessedDataAndByteJumps createOperation(JVMInstr instr, ByteData bd, ConstantPool cp, int offset) {
        return new Op01WithProcessedDataAndByteJumps(instr, null, null, offset);
    }
}
