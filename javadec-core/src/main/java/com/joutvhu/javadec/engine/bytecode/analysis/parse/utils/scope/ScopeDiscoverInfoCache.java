package com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.scope;

import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.util.collections.MapFactory;

import java.util.Map;

public class ScopeDiscoverInfoCache {
    private final Map<StructuredStatement, Boolean> tests = MapFactory.newIdentityMap();

    public Boolean get(StructuredStatement structuredStatement) {
        return tests.get(structuredStatement);
    }

    public void put(StructuredStatement structuredStatement, Boolean b) {
        tests.put(structuredStatement, b);
    }

    boolean anyFound() {
        for (Boolean value : tests.values()) {
            if (value) return true;
        }
        return false;
    }
}
