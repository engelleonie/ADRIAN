package tech.jorn.adrian.core.graphs.infrastructure;

import tech.jorn.adrian.core.graphs.AbstractDetailedNode;
import tech.jorn.adrian.core.properties.AbstractProperty;

public abstract class InfrastructureEntry<P extends AbstractProperty<?>> extends AbstractDetailedNode<P> {
    public InfrastructureEntry(String id) {
        super(id);
    }
}
