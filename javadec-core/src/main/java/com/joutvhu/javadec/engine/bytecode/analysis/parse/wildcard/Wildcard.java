package com.joutvhu.javadec.engine.bytecode.analysis.parse.wildcard;

public interface Wildcard<X> {
    X getMatch();

    void resetMatch();
}
