package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.matchutil;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.wildcard.WildcardMatch;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;

public interface MatchResultCollector {
    void clear();

    void collectStatement(String name, StructuredStatement statement);

    void collectMatches(String name, WildcardMatch wcm);
}
