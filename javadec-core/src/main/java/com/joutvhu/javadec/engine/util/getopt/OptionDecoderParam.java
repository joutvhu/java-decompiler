package com.joutvhu.javadec.engine.util.getopt;

import com.joutvhu.javadec.engine.util.functors.TrinaryFunction;

public interface OptionDecoderParam<T, ARG> extends TrinaryFunction<String, ARG, Options, T> {
    String getRangeDescription();

    String getDefaultValue();
}