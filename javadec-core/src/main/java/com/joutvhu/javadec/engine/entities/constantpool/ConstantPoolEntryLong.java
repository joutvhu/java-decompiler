package com.joutvhu.javadec.engine.entities.constantpool;

import com.joutvhu.javadec.engine.bytecode.analysis.types.StackType;
import com.joutvhu.javadec.engine.entities.AbstractConstantPoolEntry;
import com.joutvhu.javadec.engine.util.bytestream.ByteData;
import com.joutvhu.javadec.engine.util.output.Dumper;

public class ConstantPoolEntryLong extends AbstractConstantPoolEntry implements ConstantPoolEntryLiteral {
    private final long value;

    public ConstantPoolEntryLong(ConstantPool cp, ByteData data) {
        super(cp);
        this.value = data.getLongAt(1);
    }

    @Override
    public long getRawByteLength() {
        return 9;
    }

    @Override
    public void dump(Dumper d) {
        d.print("CONSTANT_Long " + value);
    }

    @Override
    public String toString() {
        return "CONSTANT_Long[" + value + "]";
    }

    public long getValue() {
        return value;
    }

    @Override
    public StackType getStackType() {
        return StackType.LONG;
    }
}
