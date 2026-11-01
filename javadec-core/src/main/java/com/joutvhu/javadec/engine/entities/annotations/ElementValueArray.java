package com.joutvhu.javadec.engine.entities.annotations;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.StringUtils;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.List;

public class ElementValueArray implements ElementValue {
    private final List<ElementValue> content;

    public ElementValueArray(List<ElementValue> content) {
        this.content = content;
    }

    @Override
    public Dumper dump(Dumper d) {
        d.print('{');
        boolean first = true;
        for (ElementValue value : content) {
            first = StringUtils.comma(first, d);
            value.dump(d);
        }
        d.print('}');
        return d;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        for (ElementValue e : content) {
            e.collectTypeUsages(collector);
        }
    }

    @Override
    public ElementValue withTypeHint(JavaTypeInstance hint) {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        else if (obj instanceof ElementValueArray) {
            ElementValueArray other = (ElementValueArray) obj;
            return content.equals(other.content);
        }
        else {
            return false;
        }
    }
}
