package com.joutvhu.javadec.engine.entities.attributes;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.Pair;
import com.joutvhu.javadec.engine.entities.constantpool.ConstantPool;
import com.joutvhu.javadec.engine.entities.annotations.ElementValue;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.bytestream.ByteData;
import com.joutvhu.javadec.engine.util.output.Dumper;

public class AttributeAnnotationDefault extends Attribute {
    public static final String ATTRIBUTE_NAME = "AnnotationDefault";

    private final ElementValue elementValue;

    public AttributeAnnotationDefault(ByteData raw, ConstantPool cp) {
        Pair<Long, ElementValue> tmp = AnnotationHelpers.getElementValue(raw, 0, cp);
        this.elementValue = tmp.getSecond();
    }

    @Override
    public String getRawName() {
        return ATTRIBUTE_NAME;
    }

    @Override
    public Dumper dump(Dumper d) {
        return elementValue.dump(d);
    }

    @Override
    public String toString() {
        return "Annotationdefault : " + elementValue;
    }

    public ElementValue getElementValue() {
        return elementValue;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        elementValue.collectTypeUsages(collector);
    }
}
