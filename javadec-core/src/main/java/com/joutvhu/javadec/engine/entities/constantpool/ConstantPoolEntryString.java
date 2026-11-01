package com.joutvhu.javadec.engine.entities.constantpool;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.QuotingUtils;
import com.joutvhu.javadec.engine.bytecode.analysis.types.StackType;
import com.joutvhu.javadec.engine.entities.AbstractConstantPoolEntry;
import com.joutvhu.javadec.engine.util.bytestream.ByteData;
import com.joutvhu.javadec.engine.util.output.Dumper;

public class ConstantPoolEntryString extends AbstractConstantPoolEntry implements ConstantPoolEntryLiteral {
    private static final long OFFSET_OF_STRING_INDEX = 1;

    private final long stringIndex;
    private transient String string;

    public ConstantPoolEntryString(ConstantPool cp, ByteData data) {
        super(cp);
        this.stringIndex = data.getU2At(OFFSET_OF_STRING_INDEX);
    }

    @Override
    public long getRawByteLength() {
        return 3;
    }

    @Override
    public void dump(Dumper d) {
        d.print("String " + getValue());
    }

    public String getValue() {
        if (string == null) {
            string = QuotingUtils.enquoteString(getCp().getUTF8Entry((int) stringIndex).getValue());
        }
        return string;
    }

    @Override
    public StackType getStackType() {
        return StackType.REF;
    }
}


