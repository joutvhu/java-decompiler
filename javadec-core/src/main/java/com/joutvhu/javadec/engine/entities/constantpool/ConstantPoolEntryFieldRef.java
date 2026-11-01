package com.joutvhu.javadec.engine.entities.constantpool;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.bytecode.analysis.types.StackType;
import com.joutvhu.javadec.engine.entities.AbstractConstantPoolEntry;
import com.joutvhu.javadec.engine.util.bytestream.ByteData;
import com.joutvhu.javadec.engine.util.output.Dumper;

public class ConstantPoolEntryFieldRef extends AbstractConstantPoolEntry {
    private static final long OFFSET_OF_CLASS_INDEX = 1;
    private static final long OFFSET_OF_NAME_AND_TYPE_INDEX = 3;

    final int classIndex;
    final int nameAndTypeIndex;
    JavaTypeInstance cachedDecodedType;

    public ConstantPoolEntryFieldRef(ConstantPool cp, ByteData data) {
        super(cp);
        this.classIndex = data.getU2At(OFFSET_OF_CLASS_INDEX);
        this.nameAndTypeIndex = data.getU2At(OFFSET_OF_NAME_AND_TYPE_INDEX);
    }

    @Override
    public long getRawByteLength() {
        return 5;
    }

    @Override
    public void dump(Dumper d) {
        ConstantPool cp = getCp();
        d.print("Field " +
                cp.getNameAndTypeEntry(nameAndTypeIndex).getName().getValue() + ":" +
                getJavaTypeInstance());
    }

    public ConstantPoolEntryClass getClassEntry() {
        return getCp().getClassEntry(classIndex);
    }

    public ConstantPoolEntryNameAndType getNameAndTypeEntry() {
        return getCp().getNameAndTypeEntry(nameAndTypeIndex);
    }

    public String getLocalName() {
        return getCp().getNameAndTypeEntry(nameAndTypeIndex).getName().getValue();
    }

    public JavaTypeInstance getJavaTypeInstance() {
        if (cachedDecodedType == null) {
            // TODO : ACTUAL FIELD HAS CORRECT DATA.
            ConstantPool cp = getCp();
            cachedDecodedType = ConstantPoolUtils.decodeTypeTok(cp.getNameAndTypeEntry(nameAndTypeIndex).getDescriptor().getValue(), cp);
        }
        return cachedDecodedType;
    }

    public StackType getStackType() {
        return getJavaTypeInstance().getStackType();
    }


    @Override
    public String toString() {
        return "ConstantPool_FieldRef [classIndex:" + classIndex + ", nameAndTypeIndex:" + nameAndTypeIndex + "]";
    }
}
