package com.joutvhu.javadec.engine.entities.attributes;

import com.joutvhu.javadec.engine.util.output.Dumper;

public class AttributeSourceFile extends Attribute {
    public static final String ATTRIBUTE_NAME = "SourceFile";

    public AttributeSourceFile() {
    }

    @Override
    public String getRawName() {
        return ATTRIBUTE_NAME;
    }

    @Override
    public Dumper dump(Dumper d) {
        return d.print(ATTRIBUTE_NAME);
    }

    @Override
    public String toString() {
        return ATTRIBUTE_NAME;
    }
}
