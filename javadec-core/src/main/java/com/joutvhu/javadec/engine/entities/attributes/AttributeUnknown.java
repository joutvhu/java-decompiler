package com.joutvhu.javadec.engine.entities.attributes;

import com.joutvhu.javadec.engine.util.output.Dumper;

public class AttributeUnknown extends Attribute {
    private final String name;

    public AttributeUnknown(String name) {
        this.name = name;
    }

    @Override
    public String getRawName() {
        return name;
    }

    @Override
    public Dumper dump(Dumper d) {
        return d.print("Unknown Attribute : " + name);
    }

    @Override
    public String toString() {
        return "Unknown Attribute : " + name;
    }
}
