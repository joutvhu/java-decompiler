package com.joutvhu.javadec.engine.bytecode.opcode;

import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op01WithProcessedDataAndByteJumps;
import com.joutvhu.javadec.engine.bytecode.analysis.stack.StackDelta;
import com.joutvhu.javadec.engine.bytecode.analysis.stack.StackDeltaImpl;
import com.joutvhu.javadec.engine.bytecode.analysis.stack.StackSim;
import com.joutvhu.javadec.engine.bytecode.analysis.types.StackType;
import com.joutvhu.javadec.engine.bytecode.analysis.types.StackTypes;
import com.joutvhu.javadec.engine.entities.constantpool.ConstantPool;
import com.joutvhu.javadec.engine.entities.constantpool.ConstantPoolEntry;
import com.joutvhu.javadec.engine.entities.Method;
import com.joutvhu.javadec.engine.util.ConfusedCFRException;
import com.joutvhu.javadec.engine.util.collections.ListFactory;
import com.joutvhu.javadec.engine.util.bytestream.ByteData;

import java.util.List;

public class OperationFactoryDefault implements OperationFactory {

    public enum Handler {
        INSTANCE(new OperationFactoryDefault());

        private final OperationFactoryDefault h;

        Handler(OperationFactoryDefault h) {
            this.h = h;
        }

        public OperationFactory getHandler() {
            return h;
        }
    }

    @Override
    public StackDelta getStackDelta(JVMInstr instr, byte[] data, ConstantPoolEntry[] cpEntries,
                                    StackSim stackSim, Method method) {
        return new StackDeltaImpl(instr.getRawStackPopped(), instr.getRawStackPushed());
    }

    @Override
    public Op01WithProcessedDataAndByteJumps createOperation(JVMInstr instr, ByteData bd, ConstantPool cp, int offset) {
        byte[] args = instr.getRawLength() == 0 ? null : bd.getBytesAt(instr.getRawLength(), 1);
        return new Op01WithProcessedDataAndByteJumps(instr, args, null, offset);
    }

    /*
     * Misc helpers.
     */
    static StackTypes getStackTypes(StackSim stackSim, Integer... indexes) {
        if (indexes.length == 1) {
            return stackSim.getEntry(indexes[0]).getType().asList();
        } else {
            List<StackType> stackTypes = ListFactory.newList();
            for (Integer index : indexes) {
                stackTypes.add(stackSim.getEntry(index).getType());
            }
            return new StackTypes(stackTypes);
        }
    }

    static int getCat(StackSim stackSim, int index) {
        return stackSim.getEntry(index).getType().getComputationCategory();
    }

    static void checkCat(StackSim stackSim, int index, @SuppressWarnings("SameParameterValue") int category) {
        if (getCat(stackSim, index) != category) {
            throw new ConfusedCFRException("Expected category " + category + " at index " + index);
        }
    }

}
