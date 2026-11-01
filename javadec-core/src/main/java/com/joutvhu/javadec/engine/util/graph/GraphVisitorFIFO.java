package com.joutvhu.javadec.engine.util.graph;

import com.joutvhu.javadec.engine.util.functors.BinaryProcedure;

public class GraphVisitorFIFO<T> extends AbstractGraphVisitorFI<T> {
    public GraphVisitorFIFO(T first, BinaryProcedure<T, GraphVisitor<T>> callee) {
        super(first, callee);
    }
}
