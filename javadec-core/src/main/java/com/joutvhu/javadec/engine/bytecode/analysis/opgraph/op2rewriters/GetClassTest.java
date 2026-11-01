package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op2rewriters;

import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs;
import com.joutvhu.javadec.engine.bytecode.opcode.JVMInstr;
import com.joutvhu.javadec.engine.entities.ClassFile;

public interface GetClassTest {
    JVMInstr getInstr();

    boolean test(ClassFile classFile, Op02WithProcessedDataAndRefs item);
}
