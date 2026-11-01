package com.joutvhu.javadec.engine.entities.attributes;

import com.joutvhu.javadec.engine.util.output.Dumper;

public class AttributeSynthetic extends Attribute {
    public final static String ATTRIBUTE_NAME = "Synthetic";

    public AttributeSynthetic() {
    }

    @Override
    public String getRawName() {
        return ATTRIBUTE_NAME;
    }

    @Override
    public Dumper dump(Dumper d) {
        return d;
    }
}
