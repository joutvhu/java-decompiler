package com.joutvhu.javadec.engine.entities.attributes;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.Pair;
import com.joutvhu.javadec.engine.entities.constantpool.ConstantPool;
import com.joutvhu.javadec.engine.entities.annotations.AnnotationTableEntry;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.collections.ListFactory;
import com.joutvhu.javadec.engine.util.TypeUsageCollectable;
import com.joutvhu.javadec.engine.util.bytestream.ByteData;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.List;

public abstract class AttributeParameterAnnotations extends Attribute implements TypeUsageCollectable {

    private static final long OFFSET_OF_NUMBER_OF_PARAMETERS = 0;
    private static final long OFFSET_OF_ANNOTATION_NAME_TABLE = 1;

    private final List<List<AnnotationTableEntry>> annotationTableEntryListList;

    public AttributeParameterAnnotations(ByteData raw, ConstantPool cp) {
        short numParameters = raw.getU1At(OFFSET_OF_NUMBER_OF_PARAMETERS);
        long offset = OFFSET_OF_ANNOTATION_NAME_TABLE;
        annotationTableEntryListList = ListFactory.newList();
        for (int x = 0; x < numParameters; ++x) {
            List<AnnotationTableEntry> annotationTableEntryList = ListFactory.newList();

            int numAnnotations = raw.getU2At(offset);
            offset += 2;
            for (int y = 0; y < numAnnotations; ++y) {
                Pair<Long, AnnotationTableEntry> ape = AnnotationHelpers.getAnnotation(raw, offset, cp);
                offset = ape.getFirst();
                annotationTableEntryList.add(ape.getSecond());
            }
            annotationTableEntryListList.add(annotationTableEntryList);
        }
    }

    public List<AnnotationTableEntry> getAnnotationsForParamIdx(int idx) {
        if (idx < 0 || idx >= annotationTableEntryListList.size()) return null;
        return annotationTableEntryListList.get(idx);
    }

    @Override
    public Dumper dump(Dumper d) {
        return d;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        for (List<AnnotationTableEntry> annotationTableEntryList : annotationTableEntryListList) {
            for (AnnotationTableEntry annotationTableEntry : annotationTableEntryList) {
                annotationTableEntry.collectTypeUsages(collector);
            }
        }
    }

}
