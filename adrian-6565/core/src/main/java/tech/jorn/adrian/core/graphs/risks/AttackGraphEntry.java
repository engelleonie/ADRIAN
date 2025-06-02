package tech.jorn.adrian.core.graphs.risks;

import tech.jorn.adrian.core.graphs.AbstractDetailedNode;
import tech.jorn.adrian.core.properties.AbstractProperty;

public abstract class AttackGraphEntry<P extends AbstractProperty<?>> extends AbstractDetailedNode<P> {
    public AttackGraphEntry(String id) {
        super(id);
    }
}

