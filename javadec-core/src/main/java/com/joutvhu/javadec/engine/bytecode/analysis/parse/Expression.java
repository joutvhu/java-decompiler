package com.joutvhu.javadec.engine.bytecode.analysis.parse;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.HasByteCodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.Literal;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.DeepCloneable;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionVisitor;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.*;
import com.joutvhu.javadec.engine.bytecode.analysis.types.discovery.InferredJavaType;
import com.joutvhu.javadec.engine.entities.exceptions.ExceptionCheck;
import com.joutvhu.javadec.engine.util.TypeUsageCollectable;
import com.joutvhu.javadec.engine.util.output.DumpableWithPrecedence;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.Map;

public interface Expression extends DumpableWithPrecedence, DeepCloneable<Expression>, ComparableUnderEC, TypeUsageCollectable, HasByteCodeLoc {
    // Can /PROBABLY/ replace LValueRewriter with expression rewriter.
    Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer);

    Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags);

    Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags);

    boolean isSimple();

    void collectUsedLValues(LValueUsageCollector lValueUsageCollector);

    boolean canPushDownInto();

    Expression pushDown(Expression toPush, Expression parent);

    InferredJavaType getInferredJavaType();

    boolean equivalentUnder(Object o, EquivalenceConstraint constraint);

    boolean canThrow(ExceptionCheck caught);

    // If this expression has any side effects, other than updating stackVar/locals it MUST return null, regardless.
    Literal getComputedLiteral(Map<LValue, Literal> display);

    boolean isValidStatement();

    <T> T visit(ExpressionVisitor<T> visitor);

    @Override
    Dumper dump(Dumper d);

}
