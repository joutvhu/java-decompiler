package com.joutvhu.javadec.engine.bytecode.analysis.loc;

import com.joutvhu.javadec.engine.entities.Method;
import com.joutvhu.javadec.engine.util.collections.CollectionUtils;
import com.joutvhu.javadec.engine.util.collections.MapFactory;
import com.joutvhu.javadec.engine.util.collections.SetFactory;
import com.joutvhu.javadec.engine.util.collections.SetUtil;

import java.util.Map;
import java.util.Set;

/*
 * This implementation is not intended to be performant - revisit when functionally complete.
 */
public class BytecodeLocCollector {

    private final Map<Method, Set<Integer>> data = MapFactory.newIdentityMap();

    private Set<Integer> getForMethod(Method method) {
        Set<Integer> locs = data.get(method);
        if (locs == null) {
            locs = SetFactory.newSet();
            data.put(method, locs);
        }
        return locs;
    }

    public void add(Method method, int offset) {
        getForMethod(method).add(offset);
    }

    public void add(Method method, Set<Integer> offsets) {
        getForMethod(method).addAll(offsets);
    }

    public BytecodeLoc getLoc() {
        if (data.isEmpty()) return BytecodeLoc.NONE;
        if (data.values().size() == 1) {
            Set<Integer> s = CollectionUtils.getSingle(data.values());
            if (s.size() == 1) {
                return new BytecodeLocSimple(
                        SetUtil.getSingle(s),
                        SetUtil.getSingle(data.keySet()));
            }
        }
        return new BytecodeLocSet(data);
    }
}
