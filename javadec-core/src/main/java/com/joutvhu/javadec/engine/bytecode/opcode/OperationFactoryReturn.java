package com.joutvhu.javadec.engine.bytecode.opcode;

import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op01WithProcessedDataAndByteJumps;
import com.joutvhu.javadec.engine.entities.constantpool.ConstantPool;
import com.joutvhu.javadec.engine.util.bytestream.ByteData;

public class OperationFactoryReturn extends OperationFactoryDefault {

    @Override
    public Op01WithProcessedDataAndByteJumps createOperation(JVMInstr instr, ByteData bd, ConstantPool cp, int offset) {
        byte[] args = instr.getRawLength() == 0 ? null : bd.getBytesAt(instr.getRawLength(), 1);
        int[] targetOffsets = new int[0]; // There are no targets.
        return new Op01WithProcessedDataAndByteJumps(instr, args, targetOffsets, offset);
    }
}
