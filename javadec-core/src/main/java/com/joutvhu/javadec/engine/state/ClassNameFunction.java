package com.joutvhu.javadec.engine.state;

import java.util.Map;

public interface ClassNameFunction {
    Map<String, String> apply(Map<String, String> names);
}
