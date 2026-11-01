package com.joutvhu.javadec.engine.bytecode.opcode;

import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op01WithProcessedDataAndByteJumps;
import com.joutvhu.javadec.engine.bytecode.analysis.stack.StackDelta;
import com.joutvhu.javadec.engine.bytecode.analysis.stack.StackSim;
import com.joutvhu.javadec.engine.entities.constantpool.ConstantPool;
import com.joutvhu.javadec.engine.entities.constantpool.ConstantPoolEntry;
import com.joutvhu.javadec.engine.entities.Method;
import com.joutvhu.javadec.engine.util.bytestream.ByteData;

public interface OperationFactory {
    /*
     * When an operation is being decoded, it may need to know about the method / class it's in
     * (eg invokedynamic);
     */
    StackDelta getStackDelta(JVMInstr instr, byte[] data, ConstantPoolEntry[] cpEntries,
                             StackSim stackSim, Method method);

    Op01WithProcessedDataAndByteJumps createOperation(JVMInstr instr, ByteData bd, ConstantPool cp, int offset);
}
