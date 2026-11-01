package com.joutvhu.javadec.engine.entities.constantpool;

import com.joutvhu.javadec.engine.bytecode.analysis.types.StackType;
import com.joutvhu.javadec.engine.entities.AbstractConstantPoolEntry;
import com.joutvhu.javadec.engine.util.bytestream.ByteData;
import com.joutvhu.javadec.engine.util.output.Dumper;

public class ConstantPoolEntryFloat extends AbstractConstantPoolEntry implements ConstantPoolEntryLiteral {
    private final float value;

    public ConstantPoolEntryFloat(ConstantPool cp, ByteData data) {
        super(cp);
        this.value = data.getFloatAt(1);
    }

    @Override
    public long getRawByteLength() {
        return 5;
    }

    @Override
    public void dump(Dumper d) {
        d.print("CONSTANT_Float");
    }

    @Override
    public StackType getStackType() {
        return StackType.FLOAT;
    }

    public float getValue() {
        return value;
    }
}
