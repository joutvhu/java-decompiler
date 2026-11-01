package com.joutvhu.javadec.engine.state;

import com.joutvhu.javadec.engine.api.ClassFileSource;
import com.joutvhu.javadec.engine.apiunreleased.ClassFileSource2;
import com.joutvhu.javadec.engine.apiunreleased.JarContent;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.Pair;
import com.joutvhu.javadec.engine.util.AnalysisType;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

/*
 * Compatibility for old class file source.
 *
 * I guess I picked the wrong day to commit to an API.
 */
public class ClassFileSourceWrapper implements ClassFileSource2 {
    private final ClassFileSource classFileSource;

    public ClassFileSourceWrapper(ClassFileSource classFileSource) {
        this.classFileSource = classFileSource;
    }

    @Override
    public JarContent addJarContent(String jarPath, AnalysisType type) {
        return new JarContentImpl(
            classFileSource.addJar(jarPath),
                Collections.<String, String>emptyMap(), type);
    }

    @Override
    public void informAnalysisRelativePathDetail(String usePath, String classFilePath) {
        classFileSource.informAnalysisRelativePathDetail(usePath, classFilePath);
    }

    @Override
    public Collection<String> addJar(String jarPath) {
        return classFileSource.addJar(jarPath);
    }

    @Override
    public String getPossiblyRenamedPath(String path) {
        return classFileSource.getPossiblyRenamedPath(path);
    }

    @Override
    public Pair<byte[], String> getClassFileContent(String path) throws IOException {
        return classFileSource.getClassFileContent(path);
    }
}
