package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.matchutil;

import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;

public class KleenePlus extends KleeneN {
    public KleenePlus(Matcher<StructuredStatement> inner) {
        super(1, inner);
    }
}
