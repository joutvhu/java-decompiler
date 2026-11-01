package com.joutvhu.javadec.engine.entities.annotations;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.output.Dumper;

public class ElementValueEnum implements ElementValue {
    private final JavaTypeInstance type;
    private final String valueName;

    public ElementValueEnum(JavaTypeInstance type, String valueName) {
        this.type = type;
        this.valueName = valueName;
    }

    @Override
    public Dumper dump(Dumper d) {
        return d.dump(type).print('.').print(valueName);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(type);
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
        else if (obj instanceof ElementValueEnum) {
            ElementValueEnum other = (ElementValueEnum) obj;
            return type.equals(other.type) && valueName.equals(other.valueName);
        }
        else {
            return false;
        }
    }
}
