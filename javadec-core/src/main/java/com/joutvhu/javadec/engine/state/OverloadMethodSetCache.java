package com.joutvhu.javadec.engine.state;

import com.joutvhu.javadec.engine.bytecode.analysis.types.MethodPrototype;
import com.joutvhu.javadec.engine.entities.ClassFile;
import com.joutvhu.javadec.engine.entities.classfilehelpers.OverloadMethodSet;
import com.joutvhu.javadec.engine.util.collections.MapFactory;
import com.joutvhu.javadec.engine.util.functors.UnaryFunction;

import java.util.Map;

// The cost of retaining all overload information may become large.  Keeping it centrally allows us to flush it if in low
// memory mode.
public class OverloadMethodSetCache {
    private final Map<ClassFile, Map<MethodPrototype, OverloadMethodSet>> content = MapFactory.newLazyMap(new UnaryFunction<ClassFile, Map<MethodPrototype, OverloadMethodSet>>() {
        @Override
        public Map<MethodPrototype, OverloadMethodSet> invoke(ClassFile arg) {
            return MapFactory.newIdentityMap();
        }
    });

    public OverloadMethodSet get(ClassFile classFile, MethodPrototype methodPrototype) {
        return content.get(classFile).get(methodPrototype);
    }

    public void set(ClassFile classFile, MethodPrototype methodPrototype, OverloadMethodSet overloadMethodSet) {
        content.get(classFile).put(methodPrototype, overloadMethodSet);
    }
}
