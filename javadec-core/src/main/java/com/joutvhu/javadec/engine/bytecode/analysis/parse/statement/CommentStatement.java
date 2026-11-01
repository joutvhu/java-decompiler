package com.joutvhu.javadec.engine.bytecode.analysis.parse.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Statement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.StatementContainer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.AbstractExpression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.Literal;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.misc.Precedence;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.literal.TypedLiteral;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.CloneHelper;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.EquivalenceConstraint;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.LValueRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.LValueUsageCollector;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.SSAIdentifiers;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.StructuredComment;
import com.joutvhu.javadec.engine.bytecode.analysis.types.RawJavaType;
import com.joutvhu.javadec.engine.bytecode.analysis.types.discovery.InferredJavaType;
import com.joutvhu.javadec.engine.util.output.Dumper;

public class CommentStatement extends AbstractStatement {
    private final Expression text;

    private CommentStatement(Expression expression) {
        super(BytecodeLoc.NONE);
        this.text = expression;
    }

    public CommentStatement(String text) {
        this(new Literal(TypedLiteral.getString(text)));
    }

    public CommentStatement(Statement statement) {
        this(new StatementExpression(statement));
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return getLoc();
    }

    @Override
    public Statement deepClone(CloneHelper cloneHelper) {
        return new CommentStatement(cloneHelper.replaceOrClone(text));
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.dump(text);
    }

    @Override
    public void replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers) {
        text.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, getContainer());
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers) {
        text.applyExpressionRewriter(expressionRewriter, ssaIdentifiers, getContainer(), ExpressionRewriterFlags.RVALUE);
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
        text.collectUsedLValues(lValueUsageCollector);
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        return new StructuredComment(text);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return true;
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return true;
    }

    /*
     * The idea of a statement expression is a bit weird, but
     */
    private static class StatementExpression extends AbstractExpression {
        private Statement statement;

        private static InferredJavaType javaType = new InferredJavaType(RawJavaType.VOID, InferredJavaType.Source.EXPRESSION);

        private StatementExpression(Statement statement) {
            super(BytecodeLoc.NONE, javaType);
            this.statement = statement;
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }

        @Override
        public BytecodeLoc getCombinedLoc() {
            return getLoc();
        }

        @Override
        public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
            statement.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers);
            return this;
        }

        @Override
        public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            statement.rewriteExpressions(expressionRewriter, ssaIdentifiers);
            return this;
        }

        @Override
        public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            return applyExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
        }

        @Override
        public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
            statement.collectLValueUsage(lValueUsageCollector);
        }

        @Override
        public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
            return false;
        }

        @Override
        public Expression deepClone(CloneHelper cloneHelper) {
            return this;
        }

        @Override
        public Precedence getPrecedence() {
            return Precedence.WEAKEST;
        }

        @Override
        public Dumper dumpInner(Dumper d) {
            return d.dump(statement);
        }
    }
}
