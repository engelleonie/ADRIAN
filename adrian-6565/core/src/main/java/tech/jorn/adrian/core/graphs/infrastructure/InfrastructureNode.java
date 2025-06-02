package tech.jorn.adrian.core.graphs.infrastructure;

import tech.jorn.adrian.core.properties.NodeProperty;

public class InfrastructureNode extends InfrastructureEntry<NodeProperty<?>> {

    public InfrastructureNode(String id) {
        super(id);
    }

    @Override
    protected <T> NodeProperty<?> makeProperty(String property, T value) {
        return new NodeProperty<>(property, value);
    }
}
