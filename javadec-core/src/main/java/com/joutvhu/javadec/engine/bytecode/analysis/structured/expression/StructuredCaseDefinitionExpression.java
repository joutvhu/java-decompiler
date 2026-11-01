package com.joutvhu.javadec.engine.bytecode.analysis.structured.expression;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.transformers.ExpressionRewriterTransformer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.LValue;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Pattern;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.StatementContainer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.AbstractExpression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.ConditionalExpression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.misc.Precedence;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.CloneHelper;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.EquivalenceConstraint;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.LValueRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.LValueUsageCollector;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.SSAIdentifiers;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredScope;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.StructuredDefinition;
import com.joutvhu.javadec.engine.bytecode.analysis.types.discovery.InferredJavaType;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.annotation.Nullable;
import com.joutvhu.javadec.engine.util.output.Dumper;

/*
 * This represents a case statement at which 'content' (and any sub-component, if content is a record) is brought into scope
 * content may be filtered by predicate.
 *
 * case Foo x where x.bob > 12
 */
public class StructuredCaseDefinitionExpression extends AbstractExpression {

    @Nullable
    private final Pattern content;
    @Nullable
    private final ConditionalExpression predicate;

    public StructuredCaseDefinitionExpression(InferredJavaType inferredJavaType, Pattern content, ConditionalExpression predicate) {
        super(BytecodeLoc.TODO, inferredJavaType);
        this.content = content;
        this.predicate = predicate;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.TODO;
    }

    /*
     * This is sub optimal - we shouldn't be shallow copying here, but I don't
     * want to add deepClone to the structuredStatement.
     */
    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return this;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        if (content != null) {
            content.collectTypeUsages(collector);
        }
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
//        StructuredScope scope = new StructuredScope();
//        if (content != null) {
//            scope.add(content);
//            new ExpressionRewriterTransformer(expressionRewriter).transform(content, scope);
//        }
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return applyExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
//        // Fugly.  TODO: Fix interface.
//        if (lValueUsageCollector instanceof LValueScopeDiscoverer) {
//            LValueScopeDiscoverer scopeDiscoverer = (LValueScopeDiscoverer) lValueUsageCollector;
////            scopeDiscoverer.enterBlock(content);
//            if (content != null) {
//                content.traceLocalVariableScope(scopeDiscoverer);
//            }
////            scopeDiscoverer.leaveBlock(content);
//        }
    }


    @Override
    public Precedence getPrecedence() {
        return Precedence.WEAKEST;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        d.dump(content);
        if (predicate != null) {
            d.separator(" ");
            d.keyword("when");
            d.separator(" ");
            predicate.dump(d);
        }
        return d;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StructuredCaseDefinitionExpression that = (StructuredCaseDefinitionExpression) o;

        if ((content == null) != (that.content == null)) return false;
        if (content != null && !content.equals(that.content)) return false;
        if ((predicate == null) != (that.predicate == null)) return false;
        if (predicate != null && !predicate.equals(that.predicate)) return false;
        return true;
    }

    @Override
    public final boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) return false;
        if (o == this) return true;
        if (getClass() != o.getClass()) return false;
        StructuredCaseDefinitionExpression other = (StructuredCaseDefinitionExpression) o;
        if (!constraint.equivalent(content, other.content)) return false;
        return true;
    }

}
