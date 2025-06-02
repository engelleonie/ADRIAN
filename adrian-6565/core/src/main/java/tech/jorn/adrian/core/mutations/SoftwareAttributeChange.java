package tech.jorn.adrian.core.mutations;

import tech.jorn.adrian.core.graphs.AbstractDetailedNode;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBaseSoftwareAsset;
import tech.jorn.adrian.core.graphs.risks.AttackGraphSoftwareAsset;
import tech.jorn.adrian.core.properties.AbstractProperty;
import tech.jorn.adrian.core.risks.RiskRule;

public class SoftwareAttributeChange<N extends AbstractDetailedNode<?>, P extends AbstractProperty<?>> extends Mutation<N> {
    private final P newValue;

    public SoftwareAttributeChange(N node, P newValue, float cost, float time, RiskRule riskRule) {
        super(node, cost, time, riskRule);
        this.newValue = newValue;
    }

    public P getNewValue() {
        return newValue;
    }

    @Override
    public void apply(N node) {
        node.setProperty(this.newValue.getName(), this.newValue.getValue());
    }

    @Override
    public boolean isApplicable(N node) {
        if (!(node instanceof KnowledgeBaseSoftwareAsset || node instanceof AttackGraphSoftwareAsset)) return false;
        var current = node.getProperty(this.newValue.getName());
        System.out.println("current: " + current + " new: " + this.newValue.getValue());
        return current.map(c -> !c.equals(this.newValue.getValue()))
                .orElse(true);
    }

    @Override
    public String toString() {
        return String.format("SoftwareAttributeChange \033[4m%s\033[0m to \033[4m%s\033[0m for node \033[4m%s\033[0m", this.newValue.getName(), this.newValue.getValue(), this.getNode().getID());
    }
}
