package com.joutvhu.javadec.engine.entities.annotations;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.output.Dumper;

public class ElementValueClass implements ElementValue {
    private final JavaTypeInstance classType;

    public ElementValueClass(JavaTypeInstance classType) {
        this.classType = classType;
    }

    @Override
    public Dumper dump(Dumper d) {
        return d.dump(classType).print(".class");
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(classType);
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
        else if (obj instanceof ElementValueClass) {
            ElementValueClass other = (ElementValueClass) obj;
            return classType.equals(other.classType);
        }
        else {
            return false;
        }
    }
}
