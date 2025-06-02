package tech.jorn.adrian.core.mutations;

import tech.jorn.adrian.core.graphs.AbstractDetailedNode;
import tech.jorn.adrian.core.properties.AbstractProperty;
import tech.jorn.adrian.core.properties.NodeProperty;
import tech.jorn.adrian.core.properties.SoftwareProperty;
import tech.jorn.adrian.core.risks.RiskRule;

public class Migration<N extends AbstractDetailedNode<P>, P extends AbstractProperty<?>> extends Mutation<N> {
    private final AbstractDetailedNode<SoftwareProperty<?>> asset;
    private final N from;

    public Migration(AbstractDetailedNode<SoftwareProperty<?>> asset, N from,  N target, float cost, float time, RiskRule riskRule) {
        super(target, cost, time, riskRule);
        this.asset = asset;
        this.from = from;
    }

    @Override
    public void apply(N node) {

    }

    @Override
    public boolean isApplicable(N node) {
        return true;
    }

    public AbstractDetailedNode<SoftwareProperty<?>> getAsset() {
        return asset;
    }

    public N getFrom() {
        return from;
    }

    @Override
    public String toString() {
        return String.format("Migrating %s from %s to %s", asset.getID(), from.getID(), node.getID());
    }
}
