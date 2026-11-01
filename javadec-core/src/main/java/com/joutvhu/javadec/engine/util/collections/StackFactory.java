package com.joutvhu.javadec.engine.util.collections;

import java.util.Stack;

public class StackFactory {
    public static <X> Stack<X> newStack() {
        return new Stack<X>();
    }
}
