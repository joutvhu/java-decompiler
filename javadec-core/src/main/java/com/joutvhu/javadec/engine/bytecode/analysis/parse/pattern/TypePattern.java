package com.joutvhu.javadec.engine.bytecode.analysis.parse.pattern;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.LValue;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Pattern;
import com.joutvhu.javadec.engine.bytecode.analysis.types.discovery.InferredJavaType;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.Collections;
import java.util.List;

public class TypePattern implements Pattern {
    private final LValue lValue;

    public TypePattern(LValue lValue) {
        this.lValue = lValue;
    }

    @Override
    public InferredJavaType getInferredJavaType() {
        return lValue.getInferredJavaType();
    }

    @Override
    public Dumper dump(Dumper d, boolean defines) {
        return LValue.Creation.dump(d, lValue);
    }

    @Override
    public Dumper dump(Dumper d) {
        return dump(d, true);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collectFrom(lValue);
    }

    @Override
    public List<LValue> getDeclaredLValues() {
        return Collections.singletonList(lValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TypePattern)) return false;
        return lValue.equals(((TypePattern) o).lValue);
    }

    @Override
    public int hashCode() {
        return lValue.hashCode();
    }
}
