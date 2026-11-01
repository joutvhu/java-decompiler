package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.matchutil;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.wildcard.WildcardMatch;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;

public abstract class AbstractMatchResultIterator implements MatchResultCollector {
    @Override
    public void clear() {

    }

    @Override
    public void collectStatement(String name, StructuredStatement statement) {
    }

    @Override
    public void collectMatches(String name, WildcardMatch wcm) {
    }
}
