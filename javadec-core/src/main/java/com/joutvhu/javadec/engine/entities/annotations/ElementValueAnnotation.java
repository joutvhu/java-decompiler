package com.joutvhu.javadec.engine.entities.annotations;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.output.Dumper;

public class ElementValueAnnotation implements ElementValue {
    private final AnnotationTableEntry annotationTableEntry;

    public ElementValueAnnotation(AnnotationTableEntry annotationTableEntry) {
        this.annotationTableEntry = annotationTableEntry;
    }

    @Override
    public Dumper dump(Dumper d) {
        return annotationTableEntry.dump(d);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        annotationTableEntry.collectTypeUsages(collector);
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
        else if (obj instanceof ElementValueAnnotation) {
            ElementValueAnnotation other = (ElementValueAnnotation) obj;
            return annotationTableEntry.equals(other.annotationTableEntry);
        }
        else {
            return false;
        }
    }
}
