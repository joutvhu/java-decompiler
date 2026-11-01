package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.matchutil;

import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;

public class KleeneStar extends KleeneN {
    public KleeneStar(Matcher<StructuredStatement> inner) {
        super(0, inner);
    }
}
