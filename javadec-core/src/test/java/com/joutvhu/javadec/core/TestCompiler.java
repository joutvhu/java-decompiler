package com.joutvhu.javadec.core;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.*;

/**
 * In-memory Java compiler using javax.tools.JavaCompiler to dynamically compile
 * real Java source snippets into bytecode for exhaustive decompilation testing.
 */
public class TestCompiler {

    public static Map<String, byte[]> compile(String className, String sourceCode) {
        return compile(Map.of(className, sourceCode));
    }

    public static Map<String, byte[]> compile(Map<String, String> sources) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("System Java Compiler not found. A JDK runtime is required.");
        }

        Map<String, byte[]> outputBytecode = new HashMap<>();
        List<JavaFileObject> sourceFiles = new ArrayList<>();

        for (Map.Entry<String, String> entry : sources.entrySet()) {
            String className = entry.getKey();
            String sourceCode = entry.getValue();
            sourceFiles.add(new SimpleJavaFileObject(
                    URI.create("string:///" + className.replace('.', '/') + ".java"),
                    JavaFileObject.Kind.SOURCE
            ) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                    return sourceCode;
                }
            });
        }

        ForwardingJavaFileManager<JavaFileManager> fileManager = new ForwardingJavaFileManager<>(
                compiler.getStandardFileManager(null, null, null)
        ) {
            @Override
            public JavaFileObject getJavaFileForOutput(Location location, String name, JavaFileObject.Kind kind, FileObject sibling) {
                return new SimpleJavaFileObject(URI.create("mem:///" + name + ".class"), kind) {
                    @Override
                    public OutputStream openOutputStream() {
                        return new ByteArrayOutputStream() {
                            @Override
                            public void close() {
                                outputBytecode.put(name, toByteArray());
                            }
                        };
                    }
                };
            }
        };

        List<String> options = List.of("-parameters", "-g");
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, options, null, sourceFiles);
        boolean success = task.call();
        if (!success) {
            throw new IllegalArgumentException("Compilation failed for sources: " + sources.keySet());
        }

        return outputBytecode;
    }
}
