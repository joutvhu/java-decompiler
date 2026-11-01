package com.joutvhu.javadec.engine.bytecode.analysis.parse.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.LValue;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Statement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.LValueExpression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.CloneHelper;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.*;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.UnstructuredSwitch;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.Collections;
import java.util.Set;

public class SwitchStatement extends AbstractStatement {
    private Expression switchOn;
    private final BlockIdentifier switchBlock;
    private boolean safeExpression = false;

    SwitchStatement(BytecodeLoc loc, Expression switchOn, BlockIdentifier switchBlock) {
        super(loc);
        this.switchOn = switchOn;
        this.switchBlock = switchBlock;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, switchOn);
    }

    @Override
    public Statement deepClone(CloneHelper cloneHelper) {
        SwitchStatement res = new SwitchStatement(getLoc(), cloneHelper.replaceOrClone(switchOn), switchBlock);
        res.safeExpression = safeExpression;
        return res;
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.print("switch (").dump(switchOn).print(") { // " + switchBlock).newln();
    }

    @Override
    public void replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers) {
        switchOn = switchOn.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, getContainer());
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers) {
        switchOn = expressionRewriter.rewriteExpression(switchOn, ssaIdentifiers, getContainer(), ExpressionRewriterFlags.RVALUE);
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
        switchOn.collectUsedLValues(lValueUsageCollector);
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        return new UnstructuredSwitch(getLoc(), switchOn, switchBlock, safeExpression);
    }

    public Expression getSwitchOn() {
        return switchOn;
    }

    public void setSwitchOn(Expression switchOn) {
        this.switchOn = switchOn;
    }

    public BlockIdentifier getSwitchBlock() {
        return switchBlock;
    }

    @Override
    public final boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) return false;
        if (o == this) return true;
        if (getClass() != o.getClass()) return false;
        SwitchStatement other = (SwitchStatement) o;
        if (!constraint.equivalent(switchOn, other.switchOn)) return false;
        return true;
    }

    @Override
    public boolean fallsToNext() {
        return false;
    }

    @Override
    public Set<LValue> wantsLifetimeHint() {
        if (switchOn instanceof LValueExpression) {
            return Collections.singleton(((LValueExpression) switchOn).getLValue());
        }
        return null;
    }

    @Override
    public void setLifetimeHint(LValue lv, boolean usedInChildren) {
        if (!usedInChildren) safeExpression = true;
    }
}
