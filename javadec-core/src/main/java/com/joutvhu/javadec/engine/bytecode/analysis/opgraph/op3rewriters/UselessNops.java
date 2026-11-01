package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op3rewriters;

import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op03SimpleStatement;
import com.joutvhu.javadec.engine.util.collections.Functional;
import com.joutvhu.javadec.engine.util.functors.Predicate;

import java.util.List;

public class UselessNops {
    public static List<Op03SimpleStatement> removeUselessNops(List<Op03SimpleStatement> in) {
        return Functional.filter(in, new Predicate<Op03SimpleStatement>() {
            @Override
            public boolean test(Op03SimpleStatement in) {
                return !(in.getSources().isEmpty() && in.getTargets().isEmpty());
            }
        });
    }
}
