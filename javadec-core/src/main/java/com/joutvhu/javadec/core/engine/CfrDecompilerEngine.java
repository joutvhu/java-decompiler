package com.joutvhu.javadec.core.engine;

import com.joutvhu.javadec.core.api.DecompileOptions;
import com.joutvhu.javadec.core.api.DecompilerEngine;
import com.joutvhu.javadec.core.loader.ZipBytecodeLoader;
import com.joutvhu.javadec.engine.api.CfrDriver;
import com.joutvhu.javadec.engine.api.ClassFileSource;
import com.joutvhu.javadec.engine.api.OutputSinkFactory;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Built-in native Java decompiler engine supporting modern Java bytecode (Java 8 to 21+).
 */
public class CfrDecompilerEngine implements DecompilerEngine {

    @Override
    public String getName() {
        return "native";
    }

    @Override
    public String decompile(String className, byte[] bytecode, DecompileOptions options) {
        return decompileWithContext(className, bytecode, null, null, options);
    }

    /**
     * Decompiles a class with optional archive file context or folder context for resolving inner/anonymous classes.
     */
    public String decompileWithContext(String className, byte[] bytecode, File archiveFile, File baseDir, DecompileOptions options) {
        String cleanName = className.replace('\\', '/');
        if (cleanName.endsWith(".class")) {
            cleanName = cleanName.substring(0, cleanName.length() - 6);
        } else if (cleanName.endsWith(".java")) {
            cleanName = cleanName.substring(0, cleanName.length() - 5);
        }
        String classPath = cleanName.replace('.', '/') + ".class";

        MemoryClassFileSource source = new MemoryClassFileSource(classPath, bytecode, archiveFile, baseDir);
        MemoryOutputSinkFactory sinkFactory = new MemoryOutputSinkFactory();

        Map<String, String> cfrOptions = new HashMap<>();
        cfrOptions.put("comments", "false");
        cfrOptions.put("showversion", "false");
        if (options != null && options.showLineNumbers()) {
            cfrOptions.put("trackbytecodeloc", "true");
        }
        if (options != null && options.includeSyntheticMembers()) {
            cfrOptions.put("removeinnerclasssynthetics", "false");
        }

        CfrDriver driver = new CfrDriver.Builder()
                .withClassFileSource(source)
                .withOutputSink(sinkFactory)
                .withOptions(cfrOptions)
                .build();

        driver.analyse(Collections.singletonList(classPath));
        return sinkFactory.getOutput();
    }

    private static class MemoryClassFileSource implements ClassFileSource {
        private final String classPath;
        private final byte[] bytecode;
        private final File archiveFile;
        private final File baseDir;

        public MemoryClassFileSource(String classPath, byte[] bytecode, File archiveFile, File baseDir) {
            this.classPath = classPath;
            this.bytecode = bytecode;
            this.archiveFile = archiveFile;
            this.baseDir = baseDir;
        }

        @Override
        public void informAnalysisRelativePathDetail(String usePath, String classFilePath) {}

        @Override
        public Collection<String> addJar(String jarPath) {
            return Collections.emptyList();
        }

        @Override
        public String getPossiblyRenamedPath(String path) {
            return path;
        }

        @Override
        public Pair<byte[], String> getClassFileContent(String path) throws IOException {
            String normalizedRequested = path.replace('\\', '/');
            if (normalizedRequested.equals(classPath) || normalizedRequested.endsWith("/" + classPath)) {
                return Pair.make(bytecode, classPath);
            }

            // 1. Resolve sibling/inner classes from archive if available
            if (archiveFile != null && archiveFile.exists()) {
                try {
                    byte[] innerBytes = ZipBytecodeLoader.loadClassBytes(archiveFile, normalizedRequested);
                    return Pair.make(innerBytes, normalizedRequested);
                } catch (IOException ignored) {}
            }

            // 2. Resolve sibling/inner classes from directory on disk if available
            if (baseDir != null && baseDir.exists()) {
                File candidate = new File(baseDir, normalizedRequested);
                if (candidate.exists() && candidate.isFile()) {
                    return Pair.make(Files.readAllBytes(candidate.toPath()), normalizedRequested);
                }
                // Try simple filename match
                int lastSlash = normalizedRequested.lastIndexOf('/');
                String simpleFileName = lastSlash >= 0 ? normalizedRequested.substring(lastSlash + 1) : normalizedRequested;
                File simpleCandidate = new File(baseDir, simpleFileName);
                if (simpleCandidate.exists() && simpleCandidate.isFile()) {
                    return Pair.make(Files.readAllBytes(simpleCandidate.toPath()), simpleCandidate.getName());
                }
            }

            throw new FileNotFoundException("Cannot find classfile " + path);
        }
    }

    private static class MemoryOutputSinkFactory implements OutputSinkFactory {
        private final StringBuilder output = new StringBuilder();

        @Override
        public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> available) {
            return Collections.singletonList(SinkClass.STRING);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
            if (sinkType == SinkType.JAVA && sinkClass == SinkClass.STRING) {
                return (Sink<T>) (Sink<String>) output::append;
            }
            return ignored -> {};
        }

        public String getOutput() {
            return output.toString();
        }
    }
}
