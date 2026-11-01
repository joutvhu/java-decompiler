package com.joutvhu.javadec.engine.util.output;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.misc.Precedence;
import com.joutvhu.javadec.engine.util.Troolean;

public interface DumpableWithPrecedence extends Dumpable {

    Precedence getPrecedence();

    Dumper dumpWithOuterPrecedence(Dumper d, Precedence outerPrecedence, Troolean isLhs);

}
