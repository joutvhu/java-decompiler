package com.joutvhu.javadec.engine.util.output;

public interface ExceptionDumper {
    void noteException(String path, String comment, Exception e);
}
