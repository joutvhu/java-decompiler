package com.joutvhu.javadec.engine.entities.attributes;

import com.joutvhu.javadec.engine.entities.constantpool.ConstantPool;
import com.joutvhu.javadec.engine.entities.constantpool.ConstantPoolEntry;
import com.joutvhu.javadec.engine.entities.constantpool.ConstantPoolEntryMethodHandle;
import com.joutvhu.javadec.engine.entities.bootstrap.BootstrapMethodInfo;
import com.joutvhu.javadec.engine.util.collections.ListFactory;
import com.joutvhu.javadec.engine.util.bytestream.ByteData;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.List;

public class AttributeBootstrapMethods extends Attribute {
    public static final String ATTRIBUTE_NAME = "BootstrapMethods";

    private final List<BootstrapMethodInfo> methodInfoList;

    public AttributeBootstrapMethods(ByteData raw, ConstantPool cp) {
        this.methodInfoList = decodeMethods(raw, cp);
    }

    public BootstrapMethodInfo getBootStrapMethodInfo(int idx) {
        if (idx < 0 || idx >= methodInfoList.size()) {
            throw new IllegalArgumentException("Invalid bootstrap index.");
        }
        return methodInfoList.get(idx);
    }

    private static List<BootstrapMethodInfo> decodeMethods(ByteData raw, ConstantPool cp) {

        List<BootstrapMethodInfo> res = ListFactory.newList();
        int numMethods = raw.getU2At(0);
        long offset = 2;
        for (int x = 0; x < numMethods; ++x) {
            int methodRef = raw.getU2At(offset);
            ConstantPoolEntryMethodHandle methodHandle = cp.getMethodHandleEntry(methodRef);

            offset += 2;
            int numBootstrapArguments = raw.getU2At(offset);
            offset += 2;
            ConstantPoolEntry[] bootstrapArguments = new ConstantPoolEntry[numBootstrapArguments];
            for (int y = 0; y < numBootstrapArguments; ++y) {
                bootstrapArguments[y] = cp.getEntry(raw.getU2At(offset));
                offset += 2;
            }
            res.add(new BootstrapMethodInfo(methodHandle, bootstrapArguments, cp));
        }
        return res;
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
