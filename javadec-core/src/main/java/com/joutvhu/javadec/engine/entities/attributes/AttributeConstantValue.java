package com.joutvhu.javadec.engine.entities.attributes;

import com.joutvhu.javadec.engine.entities.constantpool.ConstantPool;
import com.joutvhu.javadec.engine.entities.constantpool.ConstantPoolEntry;
import com.joutvhu.javadec.engine.util.bytestream.ByteData;
import com.joutvhu.javadec.engine.util.output.Dumper;

public class AttributeConstantValue extends Attribute {
    public static final String ATTRIBUTE_NAME = "ConstantValue";

    private final ConstantPoolEntry value;

    public AttributeConstantValue(ByteData raw, ConstantPool cp) {
        this.value = cp.getEntry(raw.getU2At(0));
    }

    @Override
    public String getRawName() {
        return ATTRIBUTE_NAME;
    }

    @Override
    public Dumper dump(Dumper d) {
        return d.print("ConstantValue : " + value);
    }

    @Override
    public String toString() {
        return "ConstantValue : " + value;
    }

    public ConstantPoolEntry getValue() {
        return value;
    }
}
