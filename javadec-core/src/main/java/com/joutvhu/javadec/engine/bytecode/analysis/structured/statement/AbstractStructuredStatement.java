package com.joutvhu.javadec.engine.bytecode.analysis.structured.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLocFactoryImpl;
import com.joutvhu.javadec.engine.bytecode.analysis.loc.HasByteCodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.LValue;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.StatementContainer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.lvalue.LocalVariable;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.BlockIdentifier;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.scope.ScopeDiscoverInfoCache;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredScope;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.util.functors.Predicate;
import com.joutvhu.javadec.engine.util.output.ToStringDumper;

import java.util.List;
import java.util.Vector;

public abstract class AbstractStructuredStatement implements StructuredStatement {
    private BytecodeLoc loc;
    private Op04StructuredStatement container;

    public AbstractStructuredStatement(BytecodeLoc loc) {
        this.loc = loc;
    }

    @Override
    public void addLoc(HasByteCodeLoc loc) {
        if (loc.getLoc().isEmpty()) return;
        this.loc = BytecodeLocFactoryImpl.INSTANCE.combine(this, loc);
    }

    @Override
    public BytecodeLoc getLoc() {
        return loc;
    }

    @Override
    public Op04StructuredStatement getContainer() {
        return container;
    }

    @Override
    public void setContainer(Op04StructuredStatement container) {
        this.container = container;
    }

    @Override
    public void transformStructuredChildrenInReverse(StructuredStatementTransformer transformer, StructuredScope scope) {
        transformStructuredChildren(transformer, scope);
    }

    @Override
    public StructuredStatement claimBlock(Op04StructuredStatement innerBlock, BlockIdentifier blockIdentifier, Vector<BlockIdentifier> blocksCurrentlyIn) {
        return null;
    }

    @Override
    public StructuredStatement informBlockHeirachy(Vector<BlockIdentifier> blockIdentifiers) {
        return null;
    }

    @Override
    public boolean isProperlyStructured() {
        return true;
    }

    @Override
    public boolean isRecursivelyStructured() {
        return true;
    }

    /*
     * Strictly speaking, any statement is breakable foo : synchronised(this) {}
     * is perfectly legitimate.  However CFR handles labelled blocks only on loops, switches and
     * explicit block statements.  There's no loss of generality, and it's simpler.
     */

    @Override
    public BlockIdentifier getBreakableBlockOrNull() {
        return null;
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void markCreator(LValue scopedEntity, StatementContainer<StructuredStatement> hint) {
        throw new IllegalArgumentException("Shouldn't be calling markCreator on " + this);
    }

    @Override
    public boolean alwaysDefines(LValue scopedEntity) {
        return false;
    }

    @Override
    public boolean canDefine(LValue scopedEntity, ScopeDiscoverInfoCache factCache) {
        return true;
    }

    @Override
    public boolean isScopeBlock() {
        return false;
    }

    @Override
    public boolean supportsContinueBreak() {
        return false;
    }

    @Override
    public boolean supportsBreak() {
        return false;
    }

    @Override
    public List<LValue> findCreatedHere() {
        return null;
    }

    @Override
    public String suggestName(LocalVariable createdHere, Predicate<String> testNameUsedFn) {
        return null;
    }

    @Override
    public final String toString() {
        return ToStringDumper.toString(this);
    }

    @Override
    public boolean inlineable() {
        return false;
    }

    @Override
    public Op04StructuredStatement getInline() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEffectivelyNOP() {
        return false;
    }

    @Override
    public boolean canFall() {
        return true;
    }

    @Override
    public boolean fallsNopToNext() {
        return false;
    }
}
