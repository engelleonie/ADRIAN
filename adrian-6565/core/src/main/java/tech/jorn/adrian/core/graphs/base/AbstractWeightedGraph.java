package tech.jorn.adrian.core.graphs.base;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractWeightedGraph<N extends INode> extends AbstractGraph<N, WeightedLink<N>> {

    public void addEdge(N from, N to, float weight) {
        var adj = this.adjacent.getOrDefault(from, new ArrayList<>());
        adj.add(new WeightedLink<>(to, weight));
        this.adjacent.put(from, adj);
    }

    @Override
    public void addEdge(N from, N to) {
        this.addEdge(from, to, 1.0f);
    }

    public List<WeightedLink<N>> getNeighboursWithWeights(N node) {
        return this.adjacent.get(node);
    }
}

