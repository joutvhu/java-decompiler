package com.joutvhu.javadec.engine.entities.attributes;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.Pair;
import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.entities.annotations.*;
import com.joutvhu.javadec.engine.entities.constantpool.ConstantPool;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.collections.Functional;
import com.joutvhu.javadec.engine.util.collections.ListFactory;
import com.joutvhu.javadec.engine.util.functors.Predicate;
import com.joutvhu.javadec.engine.util.TypeUsageCollectable;
import com.joutvhu.javadec.engine.util.bytestream.ByteData;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.Collections;
import java.util.List;

public abstract class AttributeAnnotations extends Attribute implements TypeUsageCollectable {

    private static final long OFFSET_OF_NUMBER_OF_ANNOTATIONS = 0;
    private static final long OFFSET_OF_ANNOTATION_TABLE = 2;

    private final List<AnnotationTableEntry> annotationTableEntryList = ListFactory.newList();

    AttributeAnnotations(ByteData raw, ConstantPool cp) {
        int numAnnotations = raw.getU2At(OFFSET_OF_NUMBER_OF_ANNOTATIONS);
        long offset = OFFSET_OF_ANNOTATION_TABLE;
        for (int x = 0; x < numAnnotations; ++x) {
            Pair<Long, AnnotationTableEntry> ape = AnnotationHelpers.getAnnotation(raw, offset, cp);
            offset = ape.getFirst();
            annotationTableEntryList.add(ape.getSecond());
        }
    }

    public void hide(final JavaTypeInstance type) {
        List<AnnotationTableEntry> hideThese = Functional.filter(annotationTableEntryList, new Predicate<AnnotationTableEntry>() {
            @Override
            public boolean test(AnnotationTableEntry in) {
                return in.getClazz().equals(type);
            }
        });
        for (AnnotationTableEntry hide : hideThese) {
            hide.setHidden();
        }
    }

    @Override
    public Dumper dump(Dumper d) {
        for (AnnotationTableEntry annotationTableEntry : annotationTableEntryList) {
            if (!annotationTableEntry.isHidden()) {
                annotationTableEntry.dump(d);
                d.newln();
            }
        }
        return d;
    }

    public List<AnnotationTableEntry> getEntryList() {
        // Prevent accidental modification
        return Collections.unmodifiableList(annotationTableEntryList);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        for (AnnotationTableEntry annotationTableEntry : annotationTableEntryList) {
            annotationTableEntry.collectTypeUsages(collector);
        }
    }

    @Override
    public String toString() {
        return annotationTableEntryList.toString();
    }
}
