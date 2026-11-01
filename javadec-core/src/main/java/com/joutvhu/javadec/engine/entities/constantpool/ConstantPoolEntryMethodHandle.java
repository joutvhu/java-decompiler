package com.joutvhu.javadec.engine.entities.constantpool;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.QuotingUtils;
import com.joutvhu.javadec.engine.bytecode.analysis.types.*;
import com.joutvhu.javadec.engine.entities.AbstractConstantPoolEntry;
import com.joutvhu.javadec.engine.entities.bootstrap.MethodHandleBehaviour;
import com.joutvhu.javadec.engine.util.bytestream.ByteData;
import com.joutvhu.javadec.engine.util.output.Dumper;

public class ConstantPoolEntryMethodHandle extends AbstractConstantPoolEntry {
    private static final long OFFSET_OF_REFERENCE_KIND = 1;
    private static final long OFFSET_OF_REFERENCE_INDEX = 2;

    private final MethodHandleBehaviour referenceKind;
    private final int referenceIndex;

    public ConstantPoolEntryMethodHandle(ConstantPool cp, ByteData data) {
        super(cp);
        this.referenceKind = MethodHandleBehaviour.decode(data.getS1At(OFFSET_OF_REFERENCE_KIND));
        this.referenceIndex = data.getU2At(OFFSET_OF_REFERENCE_INDEX);
    }

    @Override
    public long getRawByteLength() {
        return 4;
    }

    @Override
    public void dump(Dumper d) {
        d.print(this.toString());
    }

    public MethodHandleBehaviour getReferenceKind() {
        return referenceKind;
    }

    public ConstantPoolEntryMethodRef getMethodRef() {
        return getCp().getMethodRefEntry(referenceIndex);
    }

    public ConstantPoolEntryFieldRef getFieldRef() {
        return getCp().getFieldRefEntry(referenceIndex);
    }

    public boolean isFieldRef() {
        switch (referenceKind) {
            case GET_FIELD:
            case GET_STATIC:
            case PUT_FIELD:
            case PUT_STATIC:
                return true;
            default:
                return false;
        }
    }

    public String getLiteralName() {
        if (isFieldRef()) {
            return QuotingUtils.enquoteString(getFieldRef().getLocalName());
        } else {
            return getMethodRef().getMethodPrototype().toString();
        }
    }

    @Override
    public String toString() {
        return "MethodHandle value=" + referenceKind + "," + referenceIndex;
    }
}
