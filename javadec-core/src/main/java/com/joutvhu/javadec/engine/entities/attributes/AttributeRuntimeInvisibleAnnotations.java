package com.joutvhu.javadec.engine.entities.attributes;

import com.joutvhu.javadec.engine.entities.constantpool.ConstantPool;
import com.joutvhu.javadec.engine.util.bytestream.ByteData;

public class AttributeRuntimeInvisibleAnnotations extends AttributeAnnotations {
    public static final String ATTRIBUTE_NAME = "RuntimeInvisibleAnnotations";

    public AttributeRuntimeInvisibleAnnotations(ByteData raw, ConstantPool cp) {
        super(raw, cp);
    }

    @Override
    public String getRawName() {
        return ATTRIBUTE_NAME;
    }
}
