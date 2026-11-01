package com.joutvhu.javadec.engine.entities.attributes;

import com.joutvhu.javadec.engine.entities.constantpool.ConstantPool;
import com.joutvhu.javadec.engine.entities.constantpool.ConstantPoolEntryClass;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.collections.ListFactory;
import com.joutvhu.javadec.engine.util.bytestream.ByteData;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.List;

public class AttributeExceptions extends Attribute {
    public final static String ATTRIBUTE_NAME = "Exceptions";

    private static final long OFFSET_OF_NUMBER_OF_EXCEPTIONS = 0;
    private static final long OFFSET_OF_EXCEPTION_TABLE = 2;
    private final List<ConstantPoolEntryClass> exceptionClassList = ListFactory.newList();

    public AttributeExceptions(ByteData raw, ConstantPool cp) {
        int numExceptions = raw.getU2At(OFFSET_OF_NUMBER_OF_EXCEPTIONS);
        long offset = OFFSET_OF_EXCEPTION_TABLE;
        for (int x = 0; x < numExceptions; ++x, offset += 2) {
            exceptionClassList.add(cp.getClassEntry(raw.getU2At(offset)));
        }
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        for (ConstantPoolEntryClass exceptionClass : exceptionClassList) {
            collector.collect(exceptionClass.getTypeInstance());
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

    public List<ConstantPoolEntryClass> getExceptionClassList() {
        return exceptionClassList;
    }
}
