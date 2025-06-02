package tech.jorn.adrian.core.graphs.knowledgebase;

import org.w3c.dom.Node;
import tech.jorn.adrian.core.graphs.AbstractDetailedNode;
import tech.jorn.adrian.core.properties.NodeProperty;

public class KnowledgeBaseNode extends KnowledgeBaseEntry<NodeProperty<?>> {
    public KnowledgeBaseNode(String id) {
        super(id);
    }

    @Override
    protected <T> NodeProperty<?> makeProperty(String property, T value) {
        return new NodeProperty<>(property, value);
    }

    public static KnowledgeBaseNode fromNode(AbstractDetailedNode<NodeProperty<?>> node) {
        return KnowledgeBaseNode.fromNode(node, KnowledgeOrigin.INDIRECT);
    }
    public static KnowledgeBaseNode fromNode(AbstractDetailedNode<NodeProperty<?>> node, KnowledgeOrigin origin) {
        var target = new KnowledgeBaseNode(node.getID());
        target.setKnowledgeOrigin(origin);
        node.getProperties().forEach(target::setFromProperty);
        return target;
    }
}
