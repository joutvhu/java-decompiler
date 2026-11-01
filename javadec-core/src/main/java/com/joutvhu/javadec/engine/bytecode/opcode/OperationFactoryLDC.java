package com.joutvhu.javadec.engine.bytecode.opcode;

import com.joutvhu.javadec.engine.bytecode.analysis.stack.StackDelta;
import com.joutvhu.javadec.engine.bytecode.analysis.stack.StackDeltaImpl;
import com.joutvhu.javadec.engine.bytecode.analysis.stack.StackSim;
import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.bytecode.analysis.types.StackType;
import com.joutvhu.javadec.engine.bytecode.analysis.types.StackTypes;
import com.joutvhu.javadec.engine.bytecode.analysis.types.TypeConstants;
import com.joutvhu.javadec.engine.entities.constantpool.*;
import com.joutvhu.javadec.engine.entities.Method;
import com.joutvhu.javadec.engine.util.ConfusedCFRException;

public class OperationFactoryLDC extends OperationFactoryCPEntry {

    @Override
    public StackDelta getStackDelta(JVMInstr instr, byte[] data, ConstantPoolEntry[] cpEntries,
                                    StackSim stackSim, Method method) {
        StackType stackType = getStackType(cpEntries[0]);
        int requiredComputationCategory = 1;
        if (stackType.getComputationCategory() != requiredComputationCategory) {
            throw new ConfusedCFRException("Got a literal, but expected a different category");
        }
        return new StackDeltaImpl(StackTypes.EMPTY, stackType.asList());
    }

    static StackType getStackType(ConstantPoolEntry cpe) {
        if (cpe instanceof ConstantPoolEntryLiteral) {
            ConstantPoolEntryLiteral constantPoolEntryLiteral = (ConstantPoolEntryLiteral) cpe;
            return constantPoolEntryLiteral.getStackType();
        }
        if (cpe instanceof ConstantPoolEntryDynamicInfo) {
            ConstantPoolEntryDynamicInfo di = (ConstantPoolEntryDynamicInfo) cpe;
            ConstantPoolEntryNameAndType nt = di.getNameAndTypeEntry();
            JavaTypeInstance type = nt.decodeTypeTok();
            return type.getStackType();
        }
        if (cpe instanceof ConstantPoolEntryMethodHandle) {
            return TypeConstants.METHOD_HANDLE.getStackType();
        }
        if (cpe instanceof ConstantPoolEntryMethodType) {
            return TypeConstants.METHOD_TYPE.getStackType();
        }
        throw new ConfusedCFRException("Expecting a ConstantPoolEntryLiteral or ConstantPoolEntryDynamicInfo");
    }
}
