package com.joutvhu.javadec.engine.bytecode.analysis.parse;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.DeepCloneable;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.*;
import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.bytecode.analysis.types.annotated.JavaAnnotatedTypeInstance;
import com.joutvhu.javadec.engine.bytecode.analysis.types.discovery.InferredJavaType;
import com.joutvhu.javadec.engine.entities.exceptions.ExceptionCheck;
import com.joutvhu.javadec.engine.util.TypeUsageCollectable;
import com.joutvhu.javadec.engine.util.output.Dumpable;
import com.joutvhu.javadec.engine.util.output.DumpableWithPrecedence;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.List;

/*
 * Patterns are used in case statement and instanceof matching.
 *
 */
public interface Pattern extends Dumpable, TypeUsageCollectable {

    InferredJavaType getInferredJavaType();

//    JavaAnnotatedTypeInstance getAnnotatedCreationType();
//
//    boolean canThrow(ExceptionCheck caught);

    Dumper dump(Dumper d, boolean defines);

    List<LValue> getDeclaredLValues();
}
