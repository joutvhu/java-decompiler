package com.joutvhu.javadec.engine.bytecode.analysis.types;

import com.joutvhu.javadec.engine.entities.constantpool.ConstantPool;

import java.util.List;
import java.util.Map;

public interface JavaGenericBaseInstance extends JavaTypeInstance {
    JavaTypeInstance getBoundInstance(GenericTypeBinder genericTypeBinder);

    boolean tryFindBinding(JavaTypeInstance other, GenericTypeBinder target);

    boolean hasUnbound();

    boolean hasL01Wildcard();

    JavaTypeInstance getWithoutL01Wildcard();

    boolean hasForeignUnbound(ConstantPool cp, int depth, boolean noWildcard, Map<String, FormalTypeParameter> externals);

    List<JavaTypeInstance> getGenericTypes();
}
