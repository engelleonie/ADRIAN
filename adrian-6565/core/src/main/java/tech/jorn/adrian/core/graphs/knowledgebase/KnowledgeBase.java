package tech.jorn.adrian.core.graphs.knowledgebase;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import tech.jorn.adrian.core.graphs.AbstractDetailedNode;
import tech.jorn.adrian.core.graphs.base.AbstractGraph;
import tech.jorn.adrian.core.graphs.base.GraphLink;
import tech.jorn.adrian.core.graphs.base.VoidNode;
import tech.jorn.adrian.core.graphs.risks.AttackGraphNode;
import tech.jorn.adrian.core.graphs.risks.AttackGraphSoftwareAsset;
import tech.jorn.adrian.core.properties.NodeProperty;
import tech.jorn.adrian.core.properties.SoftwareProperty;
import tech.jorn.adrian.core.risks.RiskReport;

public class KnowledgeBase extends AbstractGraph<KnowledgeBaseEntry<?>, GraphLink<KnowledgeBaseEntry<?>>> {
    public KnowledgeBase() {
        super();
    }

    public void processNewInformation(AbstractDetailedNode<NodeProperty<?>> origin, KnowledgeBase knowledgeBase) {
        knowledgeBase.getNodes().forEach(node -> {
            var internalNode = this.findById(node.getID());

            // If we do not yet have the node locally, or it's information is inferred (only had an ID)
            // We need to add it to the knowledge base. Of it already exist just learn from the node
            if (internalNode.isEmpty() || internalNode.get().getKnowledgeOrigin().equals(KnowledgeOrigin.INFERRED)) {
                internalNode = Optional.of(node);
            } else {
                internalNode.get().learnFrom(node);
            }
            // If it's information from the origin, it is direct knowledge
            internalNode.get().setKnowledgeOrigin(node.getID().equals(origin.getID())
                    ? KnowledgeOrigin.DIRECT
                    : node.getKnowledgeOrigin());
            this.upsertNode(internalNode.get());
        });
        knowledgeBase.getNodes().forEach(node -> {
            var internalNode = this.findById(node.getID());
            if (internalNode.isEmpty()) return;

            // We need to make sure that all links are also updated
            var internalAdjacent = this.adjacent.getOrDefault(node, new CopyOnWriteArrayList<>());
            var adjacent = knowledgeBase.adjacent.get(node);

//            internalAdjacent.removeAll(internalAdjacent);
            for (var link : adjacent) {
                var dest = this.findById(link.getNode().getID());
                if (dest.isEmpty()) continue; // Found link to non-existing node
                internalAdjacent.removeIf(l -> l.getNode().getID().equals(link.getNode().getID()));
                this.addEdge(internalNode.get(), dest.get());
                // TODO: Maybe add the inverse too?
            }
        });
    }

    public KnowledgeBase clone() {
        var clone = new KnowledgeBase();
        // TODO: copy mutable state here, so the clone can't change the internals of the original
        this.nodes.forEach(node -> {
            try {
                var knowledgeNode = (node instanceof KnowledgeBaseNode)
                        ? KnowledgeBaseNode.fromNode((AbstractDetailedNode<NodeProperty<?>>) node)
                        : KnowledgeBaseSoftwareAsset.fromNode((AbstractDetailedNode<SoftwareProperty<?>>) node);
                if (knowledgeNode != null) clone.upsertNode(knowledgeNode);
            } catch (Exception e) {
                System.out.println("error");
            }
        });
        this.adjacent.forEach((knowledgeBaseEntry, graphLinks) -> {
            var from = clone.findById(knowledgeBaseEntry.getID());
            graphLinks.forEach(link -> {
                var to = clone.findById(link.getNode().getID());
                // TODO: Check the .get()
                clone.addEdge(from.get(), to.get());
            });
        });
        return clone;
    }

    public KnowledgeBase mergeAttackGraph(RiskReport report) {
        report.graph().getNodes().forEach(node -> {
            if (node.getID().equals(VoidNode.ID)) return;

            var knowledge = this.findById(node.getID());
            if (knowledge.isPresent() && knowledge.get().getKnowledgeOrigin().equals(KnowledgeOrigin.DIRECT)) return;

            if (node instanceof AttackGraphNode n) {
                this.upsertNode(KnowledgeBaseNode.fromNode(n)
                        .setKnowledgeOrigin(KnowledgeOrigin.INDIRECT));
            } else if (node instanceof AttackGraphSoftwareAsset n) {
                this.upsertNode(KnowledgeBaseSoftwareAsset.fromNode(n)
                        .setKnowledgeOrigin(KnowledgeOrigin.INDIRECT));
            }
        });
        report.graph().getNodes().forEach(parent -> {
            var links = report.graph().getLinks(parent);
            var knowledgeParent = this.findById(parent.getID()).get();
            var neighbours = this.adjacent.getOrDefault(knowledgeParent, new ArrayList<>());

            links.forEach(link -> {
                var node = this.findById(link.getNode().getID()).get();
                var exists = neighbours.stream().anyMatch(n -> n.getNode().getID().equals(node.getID()));
                if (exists) return;
                this.addEdge(knowledgeParent, node);
            });
        });
        return this;
    }

}
