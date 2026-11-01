package com.joutvhu.javadec.engine.bytecode.analysis.parse.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Statement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.CloneHelper;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.*;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.UnstructuredTry;
import com.joutvhu.javadec.engine.entities.exceptions.ExceptionGroup;
import com.joutvhu.javadec.engine.util.collections.SetFactory;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.List;
import java.util.Set;

public class TryStatement extends AbstractStatement {
    private final ExceptionGroup exceptionGroup;
    // This is a hack. :(
    // We keep track of what mutexes this finally leaves.
    private final Set<Expression> monitors = SetFactory.newSet();

    public TryStatement(BytecodeLoc loc, ExceptionGroup exceptionGroup) {
        super(loc);
        this.exceptionGroup = exceptionGroup;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return getLoc();
    }

    public void addExitMutex(Expression e) {
        monitors.add(e);
    }

    public Set<Expression> getMonitors() {
        return monitors;
    }

    @Override
    public Statement deepClone(CloneHelper cloneHelper) {
        TryStatement res = new TryStatement(getLoc(), exceptionGroup);
        for (Expression monitor : monitors) {
            res.monitors.add(cloneHelper.replaceOrClone(monitor));
        }
        return res;
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.print("try { ").print(exceptionGroup.getTryBlockIdentifier().toString()).newln();
    }

    @Override
    public void replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers) {
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers) {
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        return new UnstructuredTry(exceptionGroup);
    }

    public BlockIdentifier getBlockIdentifier() {
        return exceptionGroup.getTryBlockIdentifier();
    }

    public List<ExceptionGroup.Entry> getEntries() {
        return exceptionGroup.getEntries();
    }

    public boolean equivalentUnder(Object other, EquivalenceConstraint constraint) {
        return this.getClass() == other.getClass();
    }
}
