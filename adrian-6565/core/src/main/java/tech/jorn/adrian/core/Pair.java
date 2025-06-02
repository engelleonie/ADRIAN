package tech.jorn.adrian.core;

import tech.jorn.adrian.core.graphs.base.INode;

public class Pair<N extends INode> {
    private final N from;
    private final N to;

    public Pair(N from, N to) {
        this.from = from;
        this.to = to;
    }

    public N getFrom() {
        return from;
    }

    public N getTo() {
        return to;
    }
}
