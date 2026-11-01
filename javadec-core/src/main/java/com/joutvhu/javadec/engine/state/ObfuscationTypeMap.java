package com.joutvhu.javadec.engine.state;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.entities.innerclass.InnerClassAttributeInfo;
import com.joutvhu.javadec.engine.util.functors.UnaryFunction;

import java.util.List;

public interface ObfuscationTypeMap {
    boolean providesInnerClassInfo();

    JavaTypeInstance get(JavaTypeInstance type);

    UnaryFunction<JavaTypeInstance, JavaTypeInstance> getter();

    List<InnerClassAttributeInfo> getInnerClassInfo(JavaTypeInstance classType);
}
