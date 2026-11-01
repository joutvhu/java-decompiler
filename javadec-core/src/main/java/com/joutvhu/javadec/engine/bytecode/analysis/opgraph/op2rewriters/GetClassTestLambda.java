package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op2rewriters;

import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs;
import com.joutvhu.javadec.engine.bytecode.analysis.types.DynamicInvokeType;
import com.joutvhu.javadec.engine.bytecode.opcode.JVMInstr;
import com.joutvhu.javadec.engine.entities.ClassFile;
import com.joutvhu.javadec.engine.entities.bootstrap.BootstrapMethodInfo;
import com.joutvhu.javadec.engine.entities.constantpool.ConstantPoolEntry;
import com.joutvhu.javadec.engine.entities.constantpool.ConstantPoolEntryInvokeDynamic;
import com.joutvhu.javadec.engine.entities.constantpool.ConstantPoolEntryMethodRef;

public class GetClassTestLambda implements GetClassTest {

    public static GetClassTest INSTANCE = new GetClassTestLambda();

    private GetClassTestLambda() {
    }

    @Override
    public JVMInstr getInstr() {
        return JVMInstr.INVOKEDYNAMIC;
    }

    @Override
    public boolean test(ClassFile classFile, Op02WithProcessedDataAndRefs item) {
        ConstantPoolEntry[] cpEntries = item.getCpEntries();
        ConstantPoolEntryInvokeDynamic invokeDynamic = (ConstantPoolEntryInvokeDynamic) cpEntries[0];

        // Should have this as a member on name and type
        int idx = invokeDynamic.getBootstrapMethodAttrIndex();

        BootstrapMethodInfo bootstrapMethodInfo = classFile.getBootstrapMethods().getBootStrapMethodInfo(idx);
        ConstantPoolEntryMethodRef methodRef = bootstrapMethodInfo.getConstantPoolEntryMethodRef();
        String methodName = methodRef.getName();

        DynamicInvokeType dynamicInvokeType = DynamicInvokeType.lookup(methodName);
        if (dynamicInvokeType == DynamicInvokeType.UNKNOWN) return false;

        return true;
    }
}
