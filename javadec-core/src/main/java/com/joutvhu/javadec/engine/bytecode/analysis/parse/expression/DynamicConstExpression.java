package com.joutvhu.javadec.engine.bytecode.analysis.parse.expression;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.StatementContainer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.misc.Precedence;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.CloneHelper;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.EquivalenceConstraint;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.LValueRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.LValueUsageCollector;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.SSAIdentifiers;
import com.joutvhu.javadec.engine.entities.constantpool.ConstantPoolEntryDynamicInfo;
import com.joutvhu.javadec.engine.util.output.Dumper;

/*
 * A dynamic constant expression isn't currently emitted by javac, so the best we can do is produce
 * something that *LOOKS* like it, and is legible.
 */
public class DynamicConstExpression extends AbstractExpression {
    private final ConstantPoolEntryDynamicInfo cpe;
    private Expression content;

    public DynamicConstExpression(BytecodeLoc loc, ConstantPoolEntryDynamicInfo cpe, Expression content) {
        super(loc, content.getInferredJavaType());
        this.cpe = cpe;
        this.content = content;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, content);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof DynamicConstExpression)) return false;
        return content.equals(((DynamicConstExpression) o).content);
    }

    public ConstantPoolEntryDynamicInfo getConstPoolEntry() {
        return cpe;
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.WEAKEST;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        d.print(" /* dynamic constant */ ").separator("(").dump(content.getInferredJavaType().getJavaTypeInstance()).separator(")").dump(content);
        return d;
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        content = content.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        return this;
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        Expression newContent = content.applyExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
        if (newContent == content) return this;
        return new DynamicConstExpression(getLoc(), cpe, newContent);
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        Expression newContent = content.applyReverseExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
        if (newContent == content) return this;
        return new DynamicConstExpression(getLoc(), cpe, newContent);
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        content.collectUsedLValues(lValueUsageCollector);
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == this) return true;
        if (o == null) return false;
        if (!(o instanceof  DynamicConstExpression)) return false;
        return content.equivalentUnder(((DynamicConstExpression) o).content, constraint);
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new DynamicConstExpression(getLoc(), cpe, content.deepClone(cloneHelper));
    }

    public Expression getContent() {
        return content;
    }
}
