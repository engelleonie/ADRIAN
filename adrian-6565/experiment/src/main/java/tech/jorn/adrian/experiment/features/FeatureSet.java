package tech.jorn.adrian.experiment.features;


import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.agents.IAgentConfiguration;
import tech.jorn.adrian.core.graphs.AbstractDetailedNode;
import tech.jorn.adrian.core.graphs.base.AbstractNode;
import tech.jorn.adrian.core.graphs.infrastructure.Infrastructure;
import tech.jorn.adrian.core.graphs.infrastructure.InfrastructureNode;
import tech.jorn.adrian.core.graphs.infrastructure.SoftwareAsset;
import tech.jorn.adrian.core.graphs.knowledgebase.*;
import tech.jorn.adrian.core.properties.SoftwareProperty;

import java.util.List;
import java.util.stream.Collectors;

public abstract class FeatureSet {
    abstract IAgent getAgent(Infrastructure infrastructure, InfrastructureNode node);


    List<String> getNeighboursFromInfrastructure(Infrastructure infrastructure, InfrastructureNode node) {
        return infrastructure.getNeighbours(node).stream()
                .filter(n -> n instanceof InfrastructureNode)
                .map(AbstractNode::getID)
                .collect(Collectors.toList());
    }

    List<SoftwareAsset> getAssetsFromInfrastructure(Infrastructure infrastructure, InfrastructureNode node) {
        return infrastructure.getNeighbours(node).stream()
                .filter(n -> n instanceof SoftwareAsset)
                .map(n -> (SoftwareAsset) n)
                .collect(Collectors.toList());
    }

    void learnFromNeighbours(Infrastructure infrastructure, InfrastructureNode node, IAgentConfiguration configuration, KnowledgeBase knowledgeBase) {
        KnowledgeBaseNode parent = KnowledgeBaseNode.fromNode(configuration.getParentNode(), KnowledgeOrigin.DIRECT);
        knowledgeBase.upsertNode(parent);

        infrastructure.getNeighbours(node).forEach(n -> {
            KnowledgeBaseEntry<?> knowledgeNode = null;
            if (n instanceof SoftwareAsset)
                knowledgeNode = KnowledgeBaseSoftwareAsset.fromNode((AbstractDetailedNode<SoftwareProperty<?>>) n);
//                if (n instanceof InfrastructureNode) knowledgeNode = new KnowledgeBaseNode(n.getID())
//                        .setKnowledgeOrigin(KnowledgeOrigin.INFERRED);
            if (knowledgeNode != null) {
                knowledgeBase.upsertNode(knowledgeNode);
                knowledgeBase.addEdge(parent, knowledgeNode);
                knowledgeBase.addEdge(knowledgeNode, parent);
            }
        });
    }
}

