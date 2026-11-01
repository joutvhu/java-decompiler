package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.transformers;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.matchutil.*;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.*;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.wildcard.WildcardMatch;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.StructuredFinally;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.StructuredIf;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.placeholder.BeginBlock;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.placeholder.EndBlock;
import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.bytecode.analysis.types.MethodPrototype;
import com.joutvhu.javadec.engine.bytecode.analysis.types.RawJavaType;
import com.joutvhu.javadec.engine.bytecode.analysis.types.TypeConstants;
import com.joutvhu.javadec.engine.bytecode.analysis.types.discovery.InferredJavaType;
import com.joutvhu.javadec.engine.entities.AccessFlagMethod;
import com.joutvhu.javadec.engine.entities.ClassFile;
import com.joutvhu.javadec.engine.entities.Method;

import java.util.List;

public class TryResourcesTransformerJ9 extends TryResourceTransformerFinally {
    public TryResourcesTransformerJ9(ClassFile classFile) {
        super(classFile);
    }

    @Override
    protected ResourceMatch findResourceFinally(Op04StructuredStatement finallyBlock) {
        if (finallyBlock == null) return null;
        StructuredFinally finalli = (StructuredFinally)finallyBlock.getStatement();
        Op04StructuredStatement content = finalli.getCatchBlock();

        WildcardMatch wcm = new WildcardMatch();
        List<StructuredStatement> structuredStatements = MiscStatementTools.linearise(content);
        if (structuredStatements == null) return null;

        InferredJavaType inferredThrowable = new InferredJavaType(TypeConstants.THROWABLE, InferredJavaType.Source.LITERAL, true);
        InferredJavaType inferredAutoclosable = new InferredJavaType(TypeConstants.AUTO_CLOSEABLE, InferredJavaType.Source.LITERAL, true);
        JavaTypeInstance clazzType = getClassFile().getClassType();

        Matcher<StructuredStatement> m = new ResetAfterTest(wcm, new MatchOneOf(
                new MatchSequence(
                        new BeginBlock(null),
                        new StructuredIf(BytecodeLoc.NONE, new ComparisonOperation(BytecodeLoc.NONE, wcm.getExpressionWildCard("resource"), Literal.NULL, CompOp.NE), null),
                        new BeginBlock(null),
                        new MatchOneOf(
                                new StructuredExpressionStatement(BytecodeLoc.NONE, wcm.getStaticFunction("fn", clazzType, RawJavaType.VOID, null,new CastExpression(BytecodeLoc.NONE, inferredThrowable, new LValueExpression(wcm.getLValueWildCard("throwable"))), new CastExpression(BytecodeLoc.NONE, inferredAutoclosable, new LValueExpression(wcm.getLValueWildCard("resource")))), false),
                                new StructuredExpressionStatement(BytecodeLoc.NONE, wcm.getStaticFunction("fn2", clazzType, RawJavaType.VOID, null,new LValueExpression(wcm.getLValueWildCard("throwable")), new CastExpression(BytecodeLoc.NONE, inferredAutoclosable, new LValueExpression(wcm.getLValueWildCard("resource")))), false),
                                new StructuredExpressionStatement(BytecodeLoc.NONE, wcm.getStaticFunction("fn3", clazzType, RawJavaType.VOID, null,new LValueExpression(wcm.getLValueWildCard("throwable")), new LValueExpression(wcm.getLValueWildCard("resource"))), false)
                        ),
                        new EndBlock(null),
                        new EndBlock(null)
                ),
                new MatchSequence(
                        new BeginBlock(null),
                        new MatchOneOf(
                                new StructuredExpressionStatement(BytecodeLoc.NONE, wcm.getStaticFunction("fn", clazzType, RawJavaType.VOID, null,new CastExpression(BytecodeLoc.NONE, inferredThrowable, new LValueExpression(wcm.getLValueWildCard("throwable"))), new CastExpression(BytecodeLoc.NONE, inferredAutoclosable, new LValueExpression(wcm.getLValueWildCard("resource")))), false),
                                new StructuredExpressionStatement(BytecodeLoc.NONE, wcm.getStaticFunction("fn2", clazzType, RawJavaType.VOID, null,new LValueExpression(wcm.getLValueWildCard("throwable")), new CastExpression(BytecodeLoc.NONE, inferredAutoclosable, new LValueExpression(wcm.getLValueWildCard("resource")))), false),
                                new StructuredExpressionStatement(BytecodeLoc.NONE, wcm.getStaticFunction("fn3", clazzType, RawJavaType.VOID, null,new LValueExpression(wcm.getLValueWildCard("throwable")), new LValueExpression(wcm.getLValueWildCard("resource"))), false)
                        ),
                        new EndBlock(null)
                )
        ));
        MatchIterator<StructuredStatement> mi = new MatchIterator<StructuredStatement>(structuredStatements);

        TryResourcesMatchResultCollector collector = new TryResourcesMatchResultCollector();
        mi.advance();
        boolean res = m.match(mi, collector);
        if (!res) return null;

        MethodPrototype prototype = collector.fn.getMethodPrototype();
        Method resourceMethod = getClassFile().getMethodByPrototypeOrNull(prototype);
        if (resourceMethod == null) return null;
        if (!resourceMethod.getAccessFlags().contains(AccessFlagMethod.ACC_FAKE_END_RESOURCE)) return null;

        return new ResourceMatch(resourceMethod, collector.resource, collector.throwable);
    }

}
