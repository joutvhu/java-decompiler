package com.joutvhu.javadec.engine.entities.constantpool;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.entities.AbstractConstantPoolEntry;
import com.joutvhu.javadec.engine.util.bytestream.ByteData;
import com.joutvhu.javadec.engine.util.output.Dumper;

public class ConstantPoolEntryMethodType extends AbstractConstantPoolEntry {
    private static final long OFFSET_OF_DESCRIPTOR_INDEX = 1;

    private final int descriptorIndex;

    public ConstantPoolEntryMethodType(ConstantPool cp, ByteData data) {
        super(cp);
        this.descriptorIndex = data.getU2At(OFFSET_OF_DESCRIPTOR_INDEX);
    }

    public ConstantPool getCp() {
        return super.getCp();
    }

    @Override
    public long getRawByteLength() {
        return 3;
    }

    @Override
    public void dump(Dumper d) {
        d.print(this.toString());
    }

    public ConstantPoolEntryUTF8 getDescriptor() {
        return getCp().getUTF8Entry(descriptorIndex);
    }

    @Override
    public String toString() {
        return "MethodType value=" + descriptorIndex;
    }
}
