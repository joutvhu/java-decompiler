package com.joutvhu.javadec.engine.entities.classfilehelpers;

import com.joutvhu.javadec.engine.entities.AccessFlag;
import com.joutvhu.javadec.engine.entities.ClassFile;
import com.joutvhu.javadec.engine.entities.ClassFileField;
import com.joutvhu.javadec.engine.state.DCCommonState;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.StringUtils;
import com.joutvhu.javadec.engine.util.collections.Functional;
import com.joutvhu.javadec.engine.util.functors.Predicate;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.List;

public class ClassFileDumperRecord extends AbstractClassFileDumper {

    private static final AccessFlag[] dumpableAccessFlagsClass = new AccessFlag[]{
            AccessFlag.ACC_PUBLIC, AccessFlag.ACC_PRIVATE, AccessFlag.ACC_PROTECTED, AccessFlag.ACC_STRICT, AccessFlag.ACC_ABSTRACT
    };

    public ClassFileDumperRecord(DCCommonState dcCommonState) {
        super(dcCommonState);
    }

    private void dumpHeader(ClassFile c, InnerClassDumpType innerClassDumpType, Dumper d) {
        d.keyword(getAccessFlagsString(c.getAccessFlags(), dumpableAccessFlagsClass));

        d.keyword("record ");
        c.dumpClassIdentity(d);
        d.print("(");
        List<ClassFileField> fields = Functional.filter(c.getFields(), new Predicate<ClassFileField>() {
                    @Override
                    public boolean test(ClassFileField in) {
                        return !in.getField().testAccessFlag(AccessFlag.ACC_STATIC);
                    }
                });
        boolean first = true;
        for (ClassFileField f : fields) {
            first = StringUtils.comma(first, d);
            f.dumpAsRecord(d, c);
        }
        d.print(") ");
        dumpImplements(d, c.getClassSignature());
    }

    @Override
    public Dumper dump(ClassFile classFile, InnerClassDumpType innerClass, Dumper d) {
        if (!d.canEmitClass(classFile.getClassType())) return d;

        if (!innerClass.isInnerClass()) {
            dumpTopHeader(classFile, d, true);
            dumpImports(d, classFile);
        }

        dumpComments(classFile, d);
        dumpAnnotations(classFile, d);
        dumpHeader(classFile, innerClass, d);
        d.separator("{").newln();
        d.indent(1);
        boolean first = true;

        List<ClassFileField> fields = classFile.getFields();
        for (ClassFileField field : fields) {
            if (!field.shouldNotDisplay()) {
                field.dump(d, classFile);
                first = false;
            }
        }
        dumpMethods(classFile, d, first, true);
        classFile.dumpNamedInnerClasses(d);
        d.indent(-1);
        d.separator("}").newln();

        return d;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
    }
}
