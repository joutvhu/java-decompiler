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

public class OperationFactoryFakeCatch extends OperationFactoryDefault {

    @Override
    public StackDelta getStackDelta(JVMInstr instr, byte[] data, ConstantPoolEntry[] cpEntries,
                                    StackSim stackSim, Method method) {
        StackTypes pushed = StackType.REF.asList();
        List<StackType> popped = ListFactory.newList();
        for (int x = 0; x < stackSim.getDepth(); ++x) {
            popped.add(stackSim.getEntry(x).getType());
        }
        return new StackDeltaImpl(new StackTypes(popped), pushed);
    }

    @Override
    public Op01WithProcessedDataAndByteJumps createOperation(JVMInstr instr, ByteData bd, ConstantPool cp, int offset) {
        throw new ConfusedCFRException("Fake catch should never be created from bytecode");
    }
}
