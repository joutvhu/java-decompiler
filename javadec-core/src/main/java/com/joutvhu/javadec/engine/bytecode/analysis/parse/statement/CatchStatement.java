package com.joutvhu.javadec.engine.bytecode.analysis.parse.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.LValue;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Statement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.lvalue.LocalVariable;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.CloneHelper;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.*;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.UnstructuredCatch;
import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.bytecode.analysis.types.discovery.InferredJavaType;
import com.joutvhu.javadec.engine.entities.exceptions.ExceptionGroup;
import com.joutvhu.javadec.engine.util.collections.Functional;
import com.joutvhu.javadec.engine.util.functors.Predicate;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.List;

public class CatchStatement extends AbstractStatement {
    private final List<ExceptionGroup.Entry> exceptions;
    private BlockIdentifier catchBlockIdent;
    private LValue catching;

    public CatchStatement(BytecodeLoc loc, List<ExceptionGroup.Entry> exceptions, LValue catching) {
        super(loc);
        this.exceptions = exceptions;
        this.catching = catching;
        if (!exceptions.isEmpty()) {
            JavaTypeInstance collapsedCatchType = determineType(exceptions);
            InferredJavaType catchType = new InferredJavaType(collapsedCatchType, InferredJavaType.Source.EXCEPTION, true);
            this.catching.getInferredJavaType().chain(catchType);
        }
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return getLoc();
    }

    private static JavaTypeInstance determineType(List<ExceptionGroup.Entry> exceptions) {
        InferredJavaType ijt = new InferredJavaType();
        ijt.chain(new InferredJavaType(exceptions.get(0).getCatchType(), InferredJavaType.Source.EXCEPTION));
        for (int x = 1, len = exceptions.size(); x < len; ++x) {
            ijt.chain(new InferredJavaType(exceptions.get(x).getCatchType(), InferredJavaType.Source.EXCEPTION));
        }
        if (ijt.isClash()) {
            ijt.collapseTypeClash();
        }
        return ijt.getJavaTypeInstance();
    }

    @Override
    public Statement deepClone(CloneHelper cloneHelper) {
        // TODO: blockidents when cloning.
        CatchStatement res = new CatchStatement(getLoc(), exceptions, cloneHelper.replaceOrClone(catching));
        res.setCatchBlockIdent(catchBlockIdent);
        return res;
    }

    public void removeCatchBlockFor(final BlockIdentifier tryBlockIdent) {
        List<ExceptionGroup.Entry> toRemove = Functional.filter(exceptions, new Predicate<ExceptionGroup.Entry>() {
            @Override
            public boolean test(ExceptionGroup.Entry in) {
                return in.getTryBlockIdentifier().equals(tryBlockIdent);
            }
        });
        exceptions.removeAll(toRemove);
    }

    public boolean hasCatchBlockFor(final BlockIdentifier tryBlockIdent) {
        for (ExceptionGroup.Entry entry : exceptions) {
            if (entry.getTryBlockIdentifier().equals(tryBlockIdent)) return true;
        }
        return false;
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.keyword("catch ").separator("( " + exceptions + " ").dump(catching).separator(" ) ").separator("{").newln();
    }

    public BlockIdentifier getCatchBlockIdent() {
        return catchBlockIdent;
    }

    public void setCatchBlockIdent(BlockIdentifier catchBlockIdent) {
        this.catchBlockIdent = catchBlockIdent;
    }

    @Override
    public void replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers) {
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers) {
        catching = expressionRewriter.rewriteExpression(catching, ssaIdentifiers, getContainer(), ExpressionRewriterFlags.LVALUE);
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
    }

    @Override
    public void collectLValueAssignments(LValueAssignmentCollector<Statement> lValueAssigmentCollector) {
        if (catching instanceof LocalVariable) {
            lValueAssigmentCollector.collectLocalVariableAssignment((LocalVariable) catching, this.getContainer(), null);
        }
    }

    @Override
    public LValue getCreatedLValue() {
        return catching;
    }


    public List<ExceptionGroup.Entry> getExceptions() {
        return exceptions;
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        return new UnstructuredCatch(exceptions, catchBlockIdent, catching);
    }

    @Override
    public final boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) return false;
        if (o == this) return true;
        if (getClass() != o.getClass()) return false;
        CatchStatement other = (CatchStatement) o;
        if (!constraint.equivalent(exceptions, other.exceptions)) return false;
        if (!constraint.equivalent(catching, other.catching)) return false;
        return true;
    }

}
