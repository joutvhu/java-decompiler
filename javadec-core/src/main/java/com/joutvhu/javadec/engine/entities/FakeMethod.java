package com.joutvhu.javadec.engine.entities;

import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.DecompilerComments;
import com.joutvhu.javadec.engine.util.TypeUsageCollectable;
import com.joutvhu.javadec.engine.util.collections.CollectionUtils;
import com.joutvhu.javadec.engine.util.output.Dumpable;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.EnumSet;

/*
 * Not a real method - a method we've had to introduce in order to handle something that can't be properly represented
 * in java.
 */
public class FakeMethod implements TypeUsageCollectable, Dumpable {
    private final String name;
    private final EnumSet<AccessFlagMethod> accessFlags;
    private final JavaTypeInstance returnType;
    private final Op04StructuredStatement structuredStatement;
    private final DecompilerComments comments;

    public FakeMethod(String name, EnumSet<AccessFlagMethod> accessFlags, JavaTypeInstance returnType, Op04StructuredStatement structuredStatement, DecompilerComments comments) {
        this.name = name;
        this.accessFlags = accessFlags;
        this.returnType = returnType;
        this.structuredStatement = structuredStatement;
        this.comments = comments;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {

    }

    @Override
    public Dumper dump(Dumper d) {
        if (comments != null) {
            comments.dump(d);
        }
        if (accessFlags != null) {
            String prefix = CollectionUtils.join(accessFlags, " ");
            if (!prefix.isEmpty()) {
                d.keyword(prefix);
                d.separator(" ");
            }
        }
        // At this point there's no need for any implementations that carry arguments.
        d.dump(returnType).separator(" ").methodName(name, null, false, true).separator("() ");
        structuredStatement.dump(d);
        return d;
    }

    public String getName() {
        return name;
    }
}
