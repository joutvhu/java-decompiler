package com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.LValue;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Statement;
import com.joutvhu.javadec.engine.util.collections.ListFactory;
import com.joutvhu.javadec.engine.util.collections.MapFactory;

import java.util.List;
import java.util.Map;

public class CloneHelper {
    private final Map<Expression, Expression> expressionMap;
    private final Map<LValue, LValue> lValueMap;

    public CloneHelper() {
        expressionMap = MapFactory.newMap();
        lValueMap = MapFactory.newMap();

    }

    public CloneHelper(Map<Expression, Expression> expressionMap, Map<LValue, LValue> lValueMap) {
        this.expressionMap = expressionMap;
        this.lValueMap = lValueMap;
    }

    public CloneHelper(Map<Expression, Expression> expressionMap) {
        this.expressionMap = expressionMap;
        this.lValueMap = MapFactory.newMap();
    }

    public <X extends DeepCloneable<X>> List<X> replaceOrClone(List<X> in) {
        List<X> res = ListFactory.newList();
        for (X i : in) {
            res.add(i.outerDeepClone(this));
        }
        return res;
    }

    public Expression replaceOrClone(Expression source) {
        Expression replacement = expressionMap.get(source);
        if (replacement == null) {
            if (source == null) return null;
            return source.deepClone(this);
        }
        return replacement;
    }

    public LValue replaceOrClone(LValue source) {
        LValue replacement = lValueMap.get(source);
        if (replacement == null) return source.deepClone(this);
        return replacement;
    }
}
