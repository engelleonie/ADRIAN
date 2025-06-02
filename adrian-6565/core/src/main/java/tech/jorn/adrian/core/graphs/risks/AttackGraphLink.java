package tech.jorn.adrian.core.graphs.risks;

import tech.jorn.adrian.core.graphs.base.GraphLink;
import tech.jorn.adrian.core.graphs.base.INode;
import tech.jorn.adrian.core.risks.Risk;

public class AttackGraphLink<N extends INode> extends GraphLink<N> {
    private final Risk risk;

    public AttackGraphLink(N node, Risk risk) {
        super(node);
        this.risk = risk;
    }

    public Risk getRisk() {
        return this.risk;
    }
}
