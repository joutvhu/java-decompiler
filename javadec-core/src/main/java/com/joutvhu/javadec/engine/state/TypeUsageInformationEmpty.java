package com.joutvhu.javadec.engine.state;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaRefTypeInstance;
import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.util.collections.SetFactory;
import com.joutvhu.javadec.engine.util.output.IllegalIdentifierDump;
import com.joutvhu.javadec.engine.util.output.TypeContext;

import java.util.Collections;
import java.util.Set;

public class TypeUsageInformationEmpty implements TypeUsageInformation {
    public static final TypeUsageInformation INSTANCE = new TypeUsageInformationEmpty();

    @Override
    public JavaRefTypeInstance getAnalysisType() {
        return null;
    }

    @Override
    public IllegalIdentifierDump getIid() {
        return null;
    }

    @Override
    public boolean isStaticImport(JavaTypeInstance clazz, String fixedName) {
        return false;
    }

    @Override
    public Set<DetectedStaticImport> getDetectedStaticImports() {
        return Collections.emptySet();
    }

    @Override
    public Set<JavaRefTypeInstance> getUsedClassTypes() {
        return SetFactory.newOrderedSet();
    }

    @Override
    public Set<JavaRefTypeInstance> getUsedInnerClassTypes() {
        return SetFactory.newOrderedSet();
    }

    @Override
    public Set<JavaRefTypeInstance> getShortenedClassTypes() {
        return SetFactory.newOrderedSet();
    }

    @Override
    public String getName(JavaTypeInstance type, TypeContext typeContext) {
        return type.getRawName();
    }

    @Override
    public boolean isNameClash(JavaTypeInstance type, String name, TypeContext typeContext) {
        return false;
    }

    @Override
    public String generateOverriddenName(JavaRefTypeInstance clazz) {
        return clazz.getRawName();
    }

    @Override
    public String generateInnerClassShortName(JavaRefTypeInstance clazz) {
        return clazz.getRawName();
    }

    @Override
    public boolean hasLocalInstance(JavaRefTypeInstance type) {
        return false;
    }
}
