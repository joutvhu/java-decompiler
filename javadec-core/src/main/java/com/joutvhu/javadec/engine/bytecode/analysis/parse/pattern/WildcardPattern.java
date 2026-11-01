package com.joutvhu.javadec.engine.bytecode.analysis.parse.pattern;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.LValue;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Pattern;
import com.joutvhu.javadec.engine.bytecode.analysis.types.discovery.InferredJavaType;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.Collections;
import java.util.List;

public class WildcardPattern implements Pattern {
    private final InferredJavaType type;

    public WildcardPattern(InferredJavaType type) {
        this.type = type;
    }

    @Override
    public InferredJavaType getInferredJavaType() {
        return type;
    }

    @Override
    public Dumper dump(Dumper d, boolean defines) {
        d.print('_');
        return d;
    }

    @Override
    public Dumper dump(Dumper d) {
        return dump(d, true);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(type.getJavaTypeInstance());
    }

    @Override
    public List<LValue> getDeclaredLValues() {
        return Collections.emptyList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WildcardPattern)) return false;
        return type.getJavaTypeInstance().equals(((WildcardPattern) o).type.getJavaTypeInstance());
    }

    @Override
    public int hashCode() {
        return type.getJavaTypeInstance().hashCode();
    }
}
