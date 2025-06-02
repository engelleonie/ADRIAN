package tech.jorn.adrian.core.graphs.risks;

import tech.jorn.adrian.core.graphs.AbstractDetailedNode;
import tech.jorn.adrian.core.properties.NodeProperty;
import tech.jorn.adrian.core.properties.SoftwareProperty;

public class AttackGraphSoftwareAsset extends AttackGraphEntry<SoftwareProperty<?>> {
    public AttackGraphSoftwareAsset(String id) {
        super(id);
    }

    @Override
    protected <T> SoftwareProperty<?> makeProperty(String property, T value) {
        return new SoftwareProperty<>(property, value);
    }

    public static AttackGraphSoftwareAsset fromNode(AbstractDetailedNode<SoftwareProperty<?>> node) {
        var target = new AttackGraphSoftwareAsset(node.getID());
        node.getProperties().forEach(target::setFromProperty);
        return target;
    }
}
