package com.joutvhu.javadec.engine.bytecode.opcode;

import com.joutvhu.javadec.engine.util.collections.ListFactory;
import com.joutvhu.javadec.engine.util.collections.MapFactory;
import com.joutvhu.javadec.engine.util.bytestream.BaseByteData;
import com.joutvhu.javadec.engine.util.bytestream.ByteData;
import com.joutvhu.javadec.engine.util.functors.UnaryFunction;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DecodedLookupSwitch implements DecodedSwitch {
    private static final int OFFSET_OF_DEFAULT = 0;
    private static final int OFFSET_OF_NUMPAIRS = 4;
    private static final int OFFSET_OF_PAIRS = 8;

    private final List<DecodedSwitchEntry> jumpTargets;

    /*
     * Note that offsetOfOriginalInstruction is data[-1]
     */
    public DecodedLookupSwitch(byte[] data, int offsetOfOriginalInstruction) {
        int curoffset = offsetOfOriginalInstruction + 1;
        int overflow = (curoffset % 4);
        int offset = overflow > 0 ? 4 - overflow : 0;

        ByteData bd = new BaseByteData(data);
        int defaultvalue = bd.getS4At(offset + OFFSET_OF_DEFAULT);
        int numpairs = bd.getS4At(offset + OFFSET_OF_NUMPAIRS);
        // Treemap so that targets are in bytecode order.
        Map<Integer, List<Integer>> uniqueTargets = MapFactory.newLazyMap(
                new TreeMap<Integer, List<Integer>>(),
                new UnaryFunction<Integer, List<Integer>>() {
                    @Override
                    public List<Integer> invoke(Integer arg) {
                        return ListFactory.newList();
                    }
                });
        uniqueTargets.get(defaultvalue).add(null);
        for (int x = 0; x < numpairs; ++x) {
            int value = bd.getS4At(offset + OFFSET_OF_PAIRS + (x * 8));
            int target = bd.getS4At(offset + OFFSET_OF_PAIRS + (x * 8) + 4);
            if (target != defaultvalue) {
                uniqueTargets.get(target).add(value);
            }
        }
        jumpTargets = ListFactory.newList();
        for (Map.Entry<Integer, List<Integer>> entry : uniqueTargets.entrySet()) {
            jumpTargets.add(new DecodedSwitchEntry(entry.getValue(), entry.getKey()));
        }
    }

    @Override
    public List<DecodedSwitchEntry> getJumpTargets() {
        return jumpTargets;
    }
}
