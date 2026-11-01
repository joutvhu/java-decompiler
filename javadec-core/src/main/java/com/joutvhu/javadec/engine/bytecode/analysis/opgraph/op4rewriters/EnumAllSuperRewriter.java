package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.SuperFunctionInvokation;

public class EnumAllSuperRewriter extends RedundantSuperRewriter {

    @Override
    protected boolean canBeNopped(SuperFunctionInvokation superInvokation) {
        return true;
    }
}
