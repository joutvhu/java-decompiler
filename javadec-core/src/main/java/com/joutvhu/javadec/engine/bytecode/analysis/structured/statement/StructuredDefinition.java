package com.joutvhu.javadec.engine.bytecode.analysis.structured.statement;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.LValue;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.lvalue.LocalVariable;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.lvalue.SentinelLocalClassLValue;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredScope;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaRefTypeInstance;
import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.entities.ClassFile;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.collections.ListFactory;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.List;

public class StructuredDefinition extends AbstractStructuredStatement {

    private LValue scopedEntity;

    public StructuredDefinition(LValue scopedEntity) {
        super(BytecodeLoc.NONE);
        this.scopedEntity = scopedEntity;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        scopedEntity.collectTypeUsages(collector);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        Class<?> clazz = scopedEntity.getClass();
        if (clazz == LocalVariable.class) {
            return LValue.Creation.dump(dumper, scopedEntity).endCodeln();
        } else if (clazz == SentinelLocalClassLValue.class) {
            JavaTypeInstance type = ((SentinelLocalClassLValue) scopedEntity).getLocalClassType().getDeGenerifiedType();
            if (type instanceof JavaRefTypeInstance) {
                ClassFile classFile = ((JavaRefTypeInstance) type).getClassFile();
                if (classFile != null) {
                    return classFile.dumpAsInlineClass(dumper);
                }
            }
        }
        return dumper;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return getLoc();
    }

    @Override
    public void transformStructuredChildren(StructuredStatementTransformer transformer, StructuredScope scope) {
    }

    @Override
    public void linearizeInto(List<StructuredStatement> out) {
        out.add(this);
    }

    @Override
    public void traceLocalVariableScope(LValueScopeDiscoverer scopeDiscoverer) {
    }

    public LValue getLvalue() {
        return scopedEntity;
    }

    @Override
    public List<LValue> findCreatedHere() {
        return ListFactory.newImmutableList(scopedEntity);
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        StructuredStatement o = matchIterator.getCurrent();
        if (!this.equals(o)) return false;
        matchIterator.advance();
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null) return false;
        if (!(o instanceof StructuredDefinition)) return false;
        StructuredDefinition other = (StructuredDefinition) o;
        if (!scopedEntity.equals(other.scopedEntity)) return false;
        return true;
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter) {
    }

}

