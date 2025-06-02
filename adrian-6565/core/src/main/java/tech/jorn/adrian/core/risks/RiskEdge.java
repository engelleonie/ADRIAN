package tech.jorn.adrian.core.risks;

import tech.jorn.adrian.core.graphs.base.AbstractNode;

public record RiskEdge(AbstractNode from, AbstractNode to, Risk risk) {
}
