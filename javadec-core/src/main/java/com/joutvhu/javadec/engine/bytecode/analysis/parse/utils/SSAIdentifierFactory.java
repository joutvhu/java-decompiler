package com.joutvhu.javadec.engine.bytecode.analysis.parse.utils;

import com.joutvhu.javadec.engine.util.collections.MapFactory;
import com.joutvhu.javadec.engine.util.functors.UnaryFunction;

import java.util.Map;

public class SSAIdentifierFactory<KEYTYPE, CMPTYPE> {
    private final Map<KEYTYPE, Integer> nextIdentFor = MapFactory.newLazyMap(
            MapFactory.<KEYTYPE, Integer>newOrderedMap(),
            new UnaryFunction<KEYTYPE, Integer>() {
                @Override
                public Integer invoke(KEYTYPE ignore) {
                    return 0;
                }
            });

    private final UnaryFunction<KEYTYPE, CMPTYPE> typeComparisonFunction;

    public SSAIdentifierFactory(UnaryFunction<KEYTYPE, CMPTYPE> typeComparisonFunction) {
        this.typeComparisonFunction = typeComparisonFunction;
    }

    public SSAIdent getIdent(KEYTYPE lValue) {
        int val = nextIdentFor.get(lValue);
        nextIdentFor.put(lValue, val + 1);
        return new SSAIdent(val, typeComparisonFunction == null ? null : typeComparisonFunction.invoke(lValue));
    }
}
