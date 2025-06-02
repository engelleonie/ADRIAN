package tech.jorn.adrian.core.graphs.base;

public class GraphLink<N extends INode> {
    protected final N node;

    public GraphLink(N node) {
        this.node = node;
    }

    public N getNode() {
        return this.node;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof INode) return this.node.equals(obj);
        return super.equals(obj);
    }
}
