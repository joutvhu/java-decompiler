package com.joutvhu.javadec.engine.bytecode.analysis.variables;

import com.joutvhu.javadec.engine.entities.constantpool.ConstantPool;
import com.joutvhu.javadec.engine.entities.attributes.AttributeLocalVariableTable;

public class VariableNamerFactory {
    public static VariableNamer getNamer(AttributeLocalVariableTable source, ConstantPool cp) {
        if (source == null) return new VariableNamerDefault();
        return new VariableNamerHinted(source.getLocalVariableEntryList(), cp);
    }
}
