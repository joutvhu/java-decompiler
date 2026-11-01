package com.joutvhu.javadec.engine.bytecode.analysis.structured.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.LValue;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.StatementContainer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.ConditionalExpression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.BlockIdentifier;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.scope.ScopeDiscoverInfoCache;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.util.output.Dumper;

public class StructuredWhile extends AbstractStructuredConditionalLoopStatement {
    public StructuredWhile(ConditionalExpression condition, Op04StructuredStatement body, BlockIdentifier block) {
        super(BytecodeLoc.NONE, condition, block, body);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, getCondition());
    }

    @Override
    public Dumper dump(Dumper dumper) {
        if (block.hasForeignReferences()) dumper.label(block.getName(), true);
        dumper.print("while (");
        if (condition == null) {
            dumper.print("true");
        } else {
            dumper.dump(condition);
        }
        dumper.print(") ");
        getBody().dump(dumper);
        return dumper;
    }

    @Override
    public void traceLocalVariableScope(LValueScopeDiscoverer scopeDiscoverer) {
        InstanceofFlowScope.traceLoopBody(this, condition, getBody(), scopeDiscoverer);
    }

    @Override
    public boolean canDefine(LValue scopedEntity, ScopeDiscoverInfoCache factCache) {
        return InstanceofFlowScope.conditionCanDefine(this, condition, scopedEntity, factCache);
    }

    @Override
    public void markCreator(LValue scopedEntity, StatementContainer<StructuredStatement> hint) {
        this.condition = InstanceofFlowScope.conditionMarkCreator(this.condition, scopedEntity);
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        StructuredStatement o = matchIterator.getCurrent();
        if (!(o instanceof StructuredWhile)) return false;
        StructuredWhile other = (StructuredWhile) o;
        if (condition == null) {
            if (other.condition != null) return false;
        } else {
            if (!condition.equals(other.condition)) return false;
        }
        if (!block.equals(other.block)) return false;
        // Don't check locality.
        matchIterator.advance();
        return true;
    }
}
