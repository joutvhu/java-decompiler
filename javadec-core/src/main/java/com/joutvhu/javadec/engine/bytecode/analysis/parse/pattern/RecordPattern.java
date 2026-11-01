package com.joutvhu.javadec.engine.bytecode.analysis.parse.pattern;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.LValue;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Pattern;
import com.joutvhu.javadec.engine.bytecode.analysis.types.discovery.InferredJavaType;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.StringUtils;
import com.joutvhu.javadec.engine.util.collections.ListFactory;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.List;

public class RecordPattern implements Pattern {
    private final InferredJavaType type;
    private final List<Pattern> params;

    public RecordPattern(InferredJavaType type, List<Pattern> params) {
        this.type = type;
        this.params = params;
    }

    @Override
    public InferredJavaType getInferredJavaType() {
        return type;
    }

    @Override
    public Dumper dump(Dumper d, boolean defines) {
        d.dump(type.getJavaTypeInstance());
        d.print('(');
        boolean first = true;
        for (Pattern p : params) {
            first = StringUtils.comma(first, d);
            p.dump(d, defines);
        }
        d.print(')');
        return d;
    }

    @Override
    public Dumper dump(Dumper d) {
        return dump(d, true);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(type.getJavaTypeInstance());
        for (Pattern p : params) {
            p.collectTypeUsages(collector);
        }
    }

    @Override
    public List<LValue> getDeclaredLValues() {
        List<LValue> out = ListFactory.newList();
        for (Pattern p : params) {
            out.addAll(p.getDeclaredLValues());
        }
        return out;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecordPattern)) return false;
        RecordPattern other = (RecordPattern) o;
        return type.getJavaTypeInstance().equals(other.type.getJavaTypeInstance())
                && params.equals(other.params);
    }

    @Override
    public int hashCode() {
        return type.getJavaTypeInstance().hashCode() * 31 + params.hashCode();
    }
}
