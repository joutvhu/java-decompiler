package com.joutvhu.javadec.engine.bytecode.analysis.loc;

import com.joutvhu.javadec.engine.entities.Method;

public class BytecodeLocFactoryStub implements BytecodeLocFactory {
    public static BytecodeLocFactory INSTANCE = new BytecodeLocFactoryStub();

    private BytecodeLocFactoryStub() {
    }

    @Override
    public BytecodeLoc at(int originalRawOffset, Method method) {
        return BytecodeLocFactory.DISABLED;
    }
}
