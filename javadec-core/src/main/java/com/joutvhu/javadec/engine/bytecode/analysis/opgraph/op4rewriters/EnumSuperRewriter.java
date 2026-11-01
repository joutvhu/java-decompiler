package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.LValue;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.CastExpression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.LValueExpression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.SuperFunctionInvokation;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.wildcard.WildcardMatch;
import com.joutvhu.javadec.engine.util.collections.ListFactory;
import com.joutvhu.javadec.engine.util.collections.SetFactory;

import java.util.List;
import java.util.Set;

public class EnumSuperRewriter extends RedundantSuperRewriter {
    @Override
    protected List<Expression> getSuperArgs(WildcardMatch wcm) {
        List<Expression> res = ListFactory.newList();
        res.add(wcm.getExpressionWildCard("enum_a"));
        res.add(wcm.getExpressionWildCard("enum_b"));
        return res;
    }

    private static LValue getLValue(WildcardMatch wcm, String name) {
        Expression e = wcm.getExpressionWildCard(name).getMatch();
        while (e instanceof CastExpression) {
            e = ((CastExpression) e).getChild();
        }
        if (!(e instanceof LValueExpression)) {
            throw new IllegalStateException();
        }
        return ((LValueExpression) e).getLValue();
    }

    protected Set<LValue> getDeclarationsToNop(WildcardMatch wcm) {
        Set<LValue> res = SetFactory.newSet();
        res.add(getLValue(wcm, "enum_a"));
        res.add(getLValue(wcm, "enum_b"));
        return res;
    }

    @Override
    protected boolean canBeNopped(SuperFunctionInvokation superInvokation) {
        return true;
    }
}
