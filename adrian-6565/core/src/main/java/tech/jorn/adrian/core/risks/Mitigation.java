package tech.jorn.adrian.core.risks;

import tech.jorn.adrian.core.graphs.base.INode;

public abstract class Mitigation {
    private final INode node;
    private final float cost;

    public Mitigation(INode node, float cost) {
        this.node = node;
        this.cost = cost;
    }

    public INode getNode() {
        return node;
    }

    public float getCost() {
        return cost;
    }
}
