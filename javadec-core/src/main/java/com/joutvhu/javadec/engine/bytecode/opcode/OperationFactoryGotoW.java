package com.joutvhu.javadec.engine.bytecode.opcode;

import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op01WithProcessedDataAndByteJumps;
import com.joutvhu.javadec.engine.entities.constantpool.ConstantPool;
import com.joutvhu.javadec.engine.util.bytestream.ByteData;

public class OperationFactoryGotoW extends OperationFactoryDefault {

    private static final long OFFSET_OF_TARGET = 1;

    @Override
    public Op01WithProcessedDataAndByteJumps createOperation(JVMInstr instr, ByteData bd, ConstantPool cp, int offset) {
        byte[] args = bd.getBytesAt(instr.getRawLength(), 1);

        int targetOffset = bd.getS4At(OFFSET_OF_TARGET);

        int[] targetOffsets = new int[1];
        targetOffsets[0] = targetOffset;

        return new Op01WithProcessedDataAndByteJumps(instr, args, targetOffsets, offset);
    }
}
