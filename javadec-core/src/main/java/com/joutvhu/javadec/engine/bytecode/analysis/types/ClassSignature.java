package com.joutvhu.javadec.engine.bytecode.analysis.types;

import com.joutvhu.javadec.engine.entities.constantpool.ConstantPool;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.collections.ListFactory;
import com.joutvhu.javadec.engine.util.TypeUsageCollectable;

import java.util.List;

public class ClassSignature implements TypeUsageCollectable {
    private final List<FormalTypeParameter> formalTypeParameters;
    private final JavaTypeInstance superClass;
    private final List<JavaTypeInstance> interfaces;

    public ClassSignature(List<FormalTypeParameter> formalTypeParameters, JavaTypeInstance superClass, List<JavaTypeInstance> interfaces) {
        this.formalTypeParameters = formalTypeParameters;
        this.superClass = superClass;
        this.interfaces = interfaces;
    }

    public List<FormalTypeParameter> getFormalTypeParameters() {
        return formalTypeParameters;
    }

    public JavaTypeInstance getSuperClass() {
        return superClass;
    }

    public List<JavaTypeInstance> getInterfaces() {
        return interfaces;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(superClass);
        collector.collectFrom(formalTypeParameters);
        collector.collect(interfaces);
    }

    // TODO : This is pointless.
    public JavaTypeInstance getThisGeneralTypeClass(JavaTypeInstance nonGenericInstance, ConstantPool cp) {
        if (nonGenericInstance instanceof JavaGenericBaseInstance) return nonGenericInstance;
        if (formalTypeParameters == null || formalTypeParameters.isEmpty()) return nonGenericInstance;
        List<JavaTypeInstance> typeParameterNames = ListFactory.newList();
        for (FormalTypeParameter formalTypeParameter : formalTypeParameters) {
            typeParameterNames.add(new JavaGenericPlaceholderTypeInstance(formalTypeParameter.getName(), cp));
        }
        JavaTypeInstance res = new JavaGenericRefTypeInstance(nonGenericInstance, typeParameterNames);
        return res;
    }
}
