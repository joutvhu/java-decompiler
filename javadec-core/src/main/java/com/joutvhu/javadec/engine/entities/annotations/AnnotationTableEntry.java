package com.joutvhu.javadec.engine.entities.annotations;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.StringUtils;
import com.joutvhu.javadec.engine.util.TypeUsageCollectable;
import com.joutvhu.javadec.engine.util.output.Dumper;
import com.joutvhu.javadec.engine.util.output.ToStringDumper;

import java.util.Map;

public class AnnotationTableEntry implements TypeUsageCollectable {
    private final JavaTypeInstance clazz;
    // Sorted map to make ordering predictable.
    private final Map<String, ElementValue> elementValueMap;
    private boolean hidden;

    public AnnotationTableEntry(JavaTypeInstance clazz, Map<String, ElementValue> elementValueMap) {
        this.clazz = clazz;
        this.elementValueMap = elementValueMap;
    }

    public void setHidden() {
        hidden = true;
    }

    public boolean isHidden() {
        return hidden;
    }

    public JavaTypeInstance getClazz() {
        return clazz;
    }

    public Dumper dump(Dumper d) {
        d.print('@').dump(clazz);
        if (elementValueMap != null && !elementValueMap.isEmpty()) {
            d.print('(');
            boolean first = true;
            for (Map.Entry<String, ElementValue> elementValueEntry : elementValueMap.entrySet()) {
                first = StringUtils.comma(first, d);
                d.print(elementValueEntry.getKey()).print('=');
                elementValueEntry.getValue().dump(d);
            }
            d.print(')');
        }
        return d;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(clazz);
        if (elementValueMap != null) {
            for (ElementValue elementValue : elementValueMap.values()) {
                elementValue.collectTypeUsages(collector);
            }
        }
    }

    public boolean isAnnotationEqual(AnnotationTableEntry other) {
        return clazz.equals(other.getClazz()) && elementValueMap.equals(other.elementValueMap);
    }

    @Override
    public String toString() {
        ToStringDumper dumper = new ToStringDumper();
        dump(dumper);
        return dumper.toString();
    }
}
