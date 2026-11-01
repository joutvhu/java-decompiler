package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.transformers;

import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.LValue;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.StatementContainer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.ConditionalExpression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.lvalue.LocalVariable;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.lvalue.StackSSALabel;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.SSAIdentifiers;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredScope;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.StructuredCatch;
import com.joutvhu.javadec.engine.bytecode.analysis.types.TypeAnnotationHelper;
import com.joutvhu.javadec.engine.bytecode.analysis.types.annotated.JavaAnnotatedTypeInstance;
import com.joutvhu.javadec.engine.entities.annotations.AnnotationTableTypeEntry;
import com.joutvhu.javadec.engine.entities.attributes.AttributeTypeAnnotations;
import com.joutvhu.javadec.engine.entities.attributes.TypeAnnotationTargetInfo;
import com.joutvhu.javadec.engine.util.DecompilerComments;
import com.joutvhu.javadec.engine.util.collections.Functional;
import com.joutvhu.javadec.engine.util.collections.ListFactory;
import com.joutvhu.javadec.engine.util.functors.Predicate;

import java.util.Collections;
import java.util.List;
import java.util.SortedMap;

import static com.joutvhu.javadec.engine.entities.attributes.TypeAnnotationEntryValue.*;

public class TypeAnnotationTransformer implements StructuredStatementTransformer, ExpressionRewriter {

    private List<AnnotationTableTypeEntry> variableAnnotations;
    private List<AnnotationTableTypeEntry> catchAnnotations;

    private final SortedMap<Integer, Integer> instrsByOffset;
    private final DecompilerComments comments;

    public TypeAnnotationTransformer(AttributeTypeAnnotations vis, AttributeTypeAnnotations invis, SortedMap<Integer, Integer> instrsByOffset, DecompilerComments comments) {
        this.instrsByOffset = instrsByOffset;
        this.comments = comments;
        this.variableAnnotations = ListFactory.combinedOptimistic(
                vis == null ? null : vis.getAnnotationsFor(type_localvar, type_resourcevar),
                invis == null ? null : invis.getAnnotationsFor(type_localvar, type_resourcevar));

        this.catchAnnotations = ListFactory.combinedOptimistic(
                vis == null ? null : vis.getAnnotationsFor(type_throws),
                invis == null ? null : invis.getAnnotationsFor(type_throws));

    }

    public void transform(Op04StructuredStatement root) {
        StructuredScope structuredScope = new StructuredScope();
        root.transform(this, structuredScope);
    }

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        in.transformStructuredChildren(this, scope);
        in.rewriteExpressions(this);
        return in;
    }

    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
    }

    @Override
    public ConditionalExpression rewriteExpression(ConditionalExpression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        Expression res = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
        return (ConditionalExpression) res;
    }

    @Override
    public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return lValue;
    }

    @Override
    public StackSSALabel rewriteExpression(StackSSALabel lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return lValue;
    }

    // TODO : this is a scan PER USE.  It's woefully expensive. (Though why on earth would code be liberally scattered with
    // annotations?)
    private List<AnnotationTableTypeEntry> getLocalVariableAnnotations(final int offset, final int slot, final int tolerance) {
        // CFR may hold the offset the variable was /created/ at, which is 1 before it becomes valid.
        List<AnnotationTableTypeEntry> entries = variableAnnotations;
        if (entries.isEmpty()) return Collections.emptyList();

        entries = Functional.filter(entries, new Predicate<AnnotationTableTypeEntry>() {
            @Override
            public boolean test(AnnotationTableTypeEntry in) {
                TypeAnnotationTargetInfo.TypeAnnotationLocalVarTarget tgt = (TypeAnnotationTargetInfo.TypeAnnotationLocalVarTarget)in.getTargetInfo();
                return tgt.matches(offset, slot, tolerance);
            }
        });
        return entries;
    }

    @Override
    public void handleStatement(StatementContainer statementContainer) {
        Object rawStatement = statementContainer.getStatement();
        if (!(rawStatement instanceof StructuredStatement)) return;
        StructuredStatement stm = (StructuredStatement)rawStatement;

        if (stm instanceof StructuredCatch) {
            handleCatchStatement((StructuredCatch)stm);
            return;
        }

        if (variableAnnotations == null) return;
        /*
         * get anything created here.
         */
        List<LValue> createdHere = stm.findCreatedHere();
        if (createdHere == null || createdHere.isEmpty()) return;

        for (LValue lValue : createdHere) {
            if (lValue instanceof LocalVariable) {
                LocalVariable localVariable = (LocalVariable)lValue;
                int offset = localVariable.getOriginalRawOffset();
                int slot = localVariable.getIdx();
                if (offset < 0 || slot < 0) continue;

                // We allow instruction BEFORE offset.
                SortedMap<Integer, Integer> heapMap = instrsByOffset.headMap(offset);
                int offsetTolerance = heapMap.isEmpty() ? 1 : offset - heapMap.lastKey();

                List<AnnotationTableTypeEntry> entries = getLocalVariableAnnotations(offset, slot, offsetTolerance);
                if (entries == null || entries.isEmpty()) continue;

                JavaAnnotatedTypeInstance annotatedTypeInstance = localVariable.getAnnotatedCreationType();
                if (annotatedTypeInstance == null) {
                    annotatedTypeInstance = localVariable.getInferredJavaType().getJavaTypeInstance().getAnnotatedInstance();
                    localVariable.setCustomCreationType(annotatedTypeInstance);
                }

                TypeAnnotationHelper.apply(annotatedTypeInstance, entries, comments);
            }
        }
    }

    private void handleCatchStatement(StructuredCatch stm) {
        // Need to link our catch back to the ORIGINAL catch index.
        // TODO : NYI - we need to link up.
    }

}
