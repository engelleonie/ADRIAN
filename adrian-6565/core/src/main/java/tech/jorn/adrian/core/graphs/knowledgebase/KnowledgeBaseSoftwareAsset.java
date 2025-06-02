package tech.jorn.adrian.core.graphs.knowledgebase;

import tech.jorn.adrian.core.graphs.AbstractDetailedNode;
import tech.jorn.adrian.core.properties.NodeProperty;
import tech.jorn.adrian.core.properties.SoftwareProperty;

public class KnowledgeBaseSoftwareAsset extends KnowledgeBaseEntry<SoftwareProperty<?>> {
    public KnowledgeBaseSoftwareAsset(String id) {
        super(id);
    }

    @Override
    protected <T> SoftwareProperty<?> makeProperty(String property, T value) {
        return new SoftwareProperty<>(property, value);
    }

    public static KnowledgeBaseSoftwareAsset fromNode(AbstractDetailedNode<SoftwareProperty<?>> node) {
        return KnowledgeBaseSoftwareAsset.fromNode(node, KnowledgeOrigin.INDIRECT);
    }
    public static KnowledgeBaseSoftwareAsset fromNode(AbstractDetailedNode<SoftwareProperty<?>> node, KnowledgeOrigin origin) {
        var target = new KnowledgeBaseSoftwareAsset(node.getID());
        target.setKnowledgeOrigin(origin);
        node.getProperties().forEach(target::setFromProperty);
        return target;
    }
}
