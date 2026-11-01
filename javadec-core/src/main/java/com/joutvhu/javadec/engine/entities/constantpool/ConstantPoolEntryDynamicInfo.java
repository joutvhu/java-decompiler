package com.joutvhu.javadec.engine.entities.constantpool;

import com.joutvhu.javadec.engine.entities.AbstractConstantPoolEntry;
import com.joutvhu.javadec.engine.util.bytestream.ByteData;
import com.joutvhu.javadec.engine.util.output.Dumper;

public class ConstantPoolEntryDynamicInfo extends AbstractConstantPoolEntry {
    private static final long OFFSET_OF_BOOTSTRAP_METHOD_ATTR_INDEX = 1;
    private static final long OFFSET_OF_NAME_AND_TYPE_INDEX = 3;

    private final int bootstrapMethodAttrIndex;
    private final int nameAndTypeIndex;

    public ConstantPoolEntryDynamicInfo(ConstantPool cp, ByteData data) {
        super(cp);
        this.bootstrapMethodAttrIndex = data.getU2At(OFFSET_OF_BOOTSTRAP_METHOD_ATTR_INDEX);
        this.nameAndTypeIndex = data.getU2At(OFFSET_OF_NAME_AND_TYPE_INDEX);
    }

    @Override
    public long getRawByteLength() {
        return 5;
    }

    @Override
    public void dump(Dumper d) {
        d.print(this.toString());
    }

    public int getBootstrapMethodAttrIndex() {
        return bootstrapMethodAttrIndex;
    }

    public ConstantPoolEntryNameAndType getNameAndTypeEntry() {
        return getCp().getNameAndTypeEntry(nameAndTypeIndex);
    }

    @Override
    public String toString() {
        return "DynamicInfo value=" + bootstrapMethodAttrIndex + "," + nameAndTypeIndex;
    }
}
