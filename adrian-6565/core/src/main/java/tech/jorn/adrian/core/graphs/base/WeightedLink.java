package tech.jorn.adrian.core.graphs.base;

public class WeightedLink<N extends INode> extends GraphLink<N> {
    private final float weight;

    public WeightedLink(N node, float weight) {
        super(node);
        this.weight = weight;
    }

    public float getWeight() {
        return weight;
    }
}
