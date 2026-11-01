package com.joutvhu.javadec.engine.entities.constantpool;

import com.joutvhu.javadec.engine.entities.AbstractConstantPoolEntry;
import com.joutvhu.javadec.engine.util.bytestream.ByteData;
import com.joutvhu.javadec.engine.util.output.Dumper;

public class ConstantPoolEntryModuleInfo extends AbstractConstantPoolEntry {
    private static final long OFFSET_OF_NAME_INDEX = 1;

    private final int nameIndex;

    ConstantPoolEntryModuleInfo(ConstantPool cp, ByteData data) {
        super(cp);
        this.nameIndex = data.getU2At(OFFSET_OF_NAME_INDEX);
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

    public ConstantPoolEntryUTF8 getName() {
        return getCp().getUTF8Entry(nameIndex);
    }

    @Override
    public String toString() {
        return "NameIndex value=" + nameIndex;
    }
}
