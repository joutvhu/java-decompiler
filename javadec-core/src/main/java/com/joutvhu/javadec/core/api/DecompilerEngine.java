package com.joutvhu.javadec.core.api;

/**
 * Interface representing a Java decompiler engine (Fernflower, CFR, etc.).
 */
public interface DecompilerEngine {
    /**
     * Unique identifier for the decompiler engine (e.g., "fernflower", "cfr").
     */
    String getName();

    /**
     * Decompiles class bytecode into Java source code.
     *
     * @param className fully qualified internal or binary class name
     * @param bytecode  raw classfile bytes
     * @param options   decompilation configuration options
     * @return decompiled Java source code
     */
    String decompile(String className, byte[] bytecode, DecompileOptions options);
}
