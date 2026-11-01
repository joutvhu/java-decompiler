package com.joutvhu.javadec.engine.bytecode;

import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.StructuredFakeDecompFailure;
import com.joutvhu.javadec.engine.util.DecompilerComment;
import com.joutvhu.javadec.engine.util.DecompilerComments;

public class AnalysisResultFromException implements AnalysisResult {
    private final Op04StructuredStatement code;
    private final DecompilerComments comments;

    public AnalysisResultFromException(Exception e, boolean dumpStackTrace) {
        this.code = new Op04StructuredStatement(new StructuredFakeDecompFailure(e, dumpStackTrace));
        this.comments = new DecompilerComments();
        comments.addComment(new DecompilerComment("Exception decompiling", e));
    }

    @Override
    public boolean isFailed() {
        return true;
    }

    @Override
    public boolean isThrown() {
        return true;
    }

    @Override
    public Op04StructuredStatement getCode() {
        return code;
    }

    @Override
    public DecompilerComments getComments() {
        return comments;
    }

    @Override
    public AnonymousClassUsage getAnonymousClassUsage() {
        return new AnonymousClassUsage();
    }
}
