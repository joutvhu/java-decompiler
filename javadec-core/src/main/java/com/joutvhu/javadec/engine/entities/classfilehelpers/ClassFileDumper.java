package com.joutvhu.javadec.engine.entities.classfilehelpers;

import com.joutvhu.javadec.engine.entities.ClassFile;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.TypeUsageCollectable;
import com.joutvhu.javadec.engine.util.output.Dumper;

public interface ClassFileDumper extends TypeUsageCollectable {
    enum InnerClassDumpType {
        NOT(false),
        INNER_CLASS(true),
        INLINE_CLASS(true);

        final boolean isInnerClass;

        InnerClassDumpType(boolean isInnerClass) {
            this.isInnerClass = isInnerClass;
        }

        public boolean isInnerClass() {
            return isInnerClass;
        }
    }

    Dumper dump(ClassFile classFile, InnerClassDumpType innerClass, Dumper d);

    /*
     * Some dumpers may need to request additional types -
     */
    void collectTypeUsages(TypeUsageCollector collector);

}
