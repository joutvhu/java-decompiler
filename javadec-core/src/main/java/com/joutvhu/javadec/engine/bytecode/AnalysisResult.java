package com.joutvhu.javadec.engine.bytecode;

import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.joutvhu.javadec.engine.util.DecompilerComments;

public interface AnalysisResult {
    boolean isFailed();
    boolean isThrown();
    Op04StructuredStatement getCode();
    DecompilerComments getComments();
    AnonymousClassUsage getAnonymousClassUsage();
}
