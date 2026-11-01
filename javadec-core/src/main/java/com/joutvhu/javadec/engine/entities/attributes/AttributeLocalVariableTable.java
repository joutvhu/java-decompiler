package com.joutvhu.javadec.engine.entities.attributes;

import com.joutvhu.javadec.engine.util.collections.ListFactory;
import com.joutvhu.javadec.engine.util.bytestream.ByteData;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.List;

public class AttributeLocalVariableTable extends Attribute {
    public final static String ATTRIBUTE_NAME = "LocalVariableTable";

    private static final long OFFSET_OF_ENTRY_COUNT = 0;
    private static final long OFFSET_OF_ENTRIES = 2;
    private final List<LocalVariableEntry> localVariableEntryList = ListFactory.newList();

    public AttributeLocalVariableTable(ByteData raw) {
        int numLocalVariables = raw.getU2At(OFFSET_OF_ENTRY_COUNT);
        long offset = OFFSET_OF_ENTRIES;
        for (int x = 0; x < numLocalVariables; ++x) {
            int startPc = raw.getU2At(offset);
            int length = raw.getU2At(offset + 2);
            int nameIndex = raw.getU2At(offset + 4);
            int descriptorIndex = raw.getU2At(offset + 6);
            int index = raw.getU2At(offset + 8);
            localVariableEntryList.add(new LocalVariableEntry(startPc, length, nameIndex, descriptorIndex, index));
            offset += 10;
        }
    }

    @Override
    public String getRawName() {
        return ATTRIBUTE_NAME;
    }

    @Override
    public Dumper dump(Dumper d) {
        return d;
    }

    public List<LocalVariableEntry> getLocalVariableEntryList() {
        return localVariableEntryList;
    }
}
