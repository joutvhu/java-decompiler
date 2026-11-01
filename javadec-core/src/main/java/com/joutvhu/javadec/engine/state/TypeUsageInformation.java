package com.joutvhu.javadec.engine.state;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaRefTypeInstance;
import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.util.output.IllegalIdentifierDump;
import com.joutvhu.javadec.engine.util.output.TypeContext;

import java.util.Set;

public interface TypeUsageInformation {
    JavaRefTypeInstance getAnalysisType();

    Set<JavaRefTypeInstance> getShortenedClassTypes();

    Set<JavaRefTypeInstance> getUsedClassTypes();

    Set<JavaRefTypeInstance> getUsedInnerClassTypes();

    boolean hasLocalInstance(JavaRefTypeInstance type);

    String getName(JavaTypeInstance type, TypeContext typeContext);

    boolean isNameClash(JavaTypeInstance type, String name, TypeContext typeContext);

    String generateInnerClassShortName(JavaRefTypeInstance clazz);

    String generateOverriddenName(JavaRefTypeInstance clazz);

    IllegalIdentifierDump getIid();

    boolean isStaticImport(JavaTypeInstance clazz, String fixedName);

    Set<DetectedStaticImport> getDetectedStaticImports();
}
