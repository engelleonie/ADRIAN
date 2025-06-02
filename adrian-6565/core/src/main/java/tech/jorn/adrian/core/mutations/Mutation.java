package tech.jorn.adrian.core.mutations;

import tech.jorn.adrian.core.graphs.AbstractDetailedNode;
import tech.jorn.adrian.core.risks.RiskRule;

public abstract class Mutation<N extends AbstractDetailedNode<?>> {
    protected final N node;
    protected final float cost;
    protected final float time;
    protected final RiskRule riskRule;

    public Mutation(N node, float cost, float time, RiskRule riskRule) {
        this.node = node;
        this.cost = cost;
        this.time = time;
        this.riskRule = riskRule;
    }

    public N getNode() {
        return node;
    }

    public float getCost() {
        return this.cost;
    }

    public float getTime() {
        return time;
    }

    public RiskRule getRiskRule() {
        return riskRule;
    }

    public abstract void apply(N node);

    public abstract boolean isApplicable(N node);

    public abstract String toString();
}

