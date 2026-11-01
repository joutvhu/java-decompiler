package com.joutvhu.javadec.engine.util.bytestream;

public interface OffsettingByteData extends ByteData {
    void advance(long offset);
    long getOffset();
}
