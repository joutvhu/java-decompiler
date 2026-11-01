package com.joutvhu.javadec.engine.apiunreleased;

import com.joutvhu.javadec.engine.util.AnalysisType;

import java.util.Collection;
import java.util.Map;

public interface JarContent {
    /**
     * @return All files which we may want to process.
     */
    Collection<String> getClassFiles();

    /**
     * Nb: Java has a perfectly good {@link java.util.jar.Manifest} class since 1.2
     * which could be used here.  This simplifies the content.
     * @return Map of manifest entries.
     */
    Map<String, String> getManifestEntries();

    /**
     * @return source of data - jar? war?
     */
    AnalysisType getAnalysisType();
}
