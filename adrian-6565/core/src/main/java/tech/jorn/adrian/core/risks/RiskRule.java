package tech.jorn.adrian.core.risks;

import tech.jorn.adrian.core.graphs.AbstractDetailedNode;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBase;
import tech.jorn.adrian.core.mutations.Mutation;
import tech.jorn.adrian.core.properties.AbstractProperty;

import java.util.Optional;
import java.util.function.Consumer;

public abstract class RiskRule {
    public abstract void evaluate(KnowledgeBase knowledgeBase, Consumer<RiskEdge> attackGraph);

    public abstract <N extends AbstractDetailedNode<P>, P extends AbstractProperty<?>> Optional<Mutation<N>> getAdaptation(N node);
}

