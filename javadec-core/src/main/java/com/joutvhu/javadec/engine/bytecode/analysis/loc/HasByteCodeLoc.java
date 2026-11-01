package com.joutvhu.javadec.engine.bytecode.analysis.loc;

public interface HasByteCodeLoc {
    BytecodeLoc getCombinedLoc();

    BytecodeLoc getLoc();

    void addLoc(HasByteCodeLoc loc);
}
