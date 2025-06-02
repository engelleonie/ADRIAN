package tech.jorn.adrian.agent.services;

import tech.jorn.adrian.core.agents.IAgentConfiguration;
import tech.jorn.adrian.core.graphs.MermaidGraphRenderer;
import tech.jorn.adrian.core.graphs.base.AbstractNode;
import tech.jorn.adrian.core.graphs.base.INode;
import tech.jorn.adrian.core.graphs.base.VoidNode;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBase;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBaseNode;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBaseSoftwareAsset;
import tech.jorn.adrian.core.graphs.risks.*;
import tech.jorn.adrian.core.risks.RiskEdge;
import tech.jorn.adrian.core.risks.RiskReport;
import tech.jorn.adrian.core.risks.RiskRule;
import tech.jorn.adrian.core.services.RiskDetection;
import tech.jorn.adrian.core.services.probability.IRiskProbabilityCalculator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BasicRiskDetection implements RiskDetection {
    private final List<RiskRule> riskRules;
    private final IRiskProbabilityCalculator probabilityCalculator;
    private final IAgentConfiguration configuration;

    public BasicRiskDetection(List<RiskRule> riskRules, IRiskProbabilityCalculator probabilityCalculator, IAgentConfiguration configuration) {
        this.riskRules = riskRules;
        this.probabilityCalculator = probabilityCalculator;
        this.configuration = configuration;
    }

    @Override
    public AttackGraph createAttackGraph(KnowledgeBase knowledgeBase) {
        var attackGraph = new AttackGraph();
        attackGraph.upsertNode(VoidNode.getIncoming());

        knowledgeBase.getNodes().forEach(node -> {
            AttackGraphEntry<?> attackNode;
            if (node instanceof KnowledgeBaseNode)
                attackNode = AttackGraphNode.fromNode((KnowledgeBaseNode) node);
            else // if (node instanceof KnowledgeBaseSoftwareAsset)
                attackNode = AttackGraphSoftwareAsset.fromNode((KnowledgeBaseSoftwareAsset) node);

            node.getProperties().forEach(attackNode::setFromProperty);
            attackGraph.upsertNode(attackNode);
        });

        Consumer<RiskEdge> dispatchRisk = this.createRiskDispatcher(attackGraph);


//        if (this.configuration != null)
//            MermaidGraphRenderer.forKnowledgeBase().toFile("./knowledge-" + this.configuration.getNodeID() + ".mmd", knowledgeBase, "");

        this.riskRules.forEach(r -> r.evaluate(knowledgeBase, dispatchRisk));
        return attackGraph;
    }

    @Override
    public List<RiskReport> identifyRisks(AttackGraph attackGraph, boolean isContained) {
        // 1. Collect all the exposed nodes and critical software components to calculate all paths.
        var voidNode = attackGraph.findById(VoidNode.getIncoming().getID());
        List<? extends AttackGraphEntry<?>> exposed = attackGraph.getNeighbours(VoidNode.getIncoming());
        List<AttackGraphSoftwareAsset> criticalSoftware = new ArrayList<>();
        attackGraph.getNodes().forEach(node -> {
            if (node instanceof AttackGraphSoftwareAsset) {
                if ((boolean) node.getProperty("isCritical").orElse(false)) {
                    criticalSoftware.add((AttackGraphSoftwareAsset) node);
                }
            }
        });

        // 2. If there are no nodes to start from or go to, we can exit.
        if (exposed.isEmpty() || criticalSoftware.isEmpty())
            return new ArrayList<>();

        // 3. Get all possible paths between all different start en end nodes.
        List<List<AttackGraphEntry<?>>> criticalPaths = new ArrayList<>();

        criticalSoftware.forEach(asset -> {
            var newPaths = attackGraph.findPathsTo(VoidNode.getIncoming(), asset).stream()
                    .filter(path -> {
                        // Check if the path contains the current node, otherwise we should not select it
                        if (this.configuration == null || !isContained) return true;
                        var contained = path.stream().filter(n -> n.getID().equals(this.configuration.getNodeID())).findAny();
                        return contained.isPresent();
                    })
                    .toList();
            var unique = new HashSet<String>();
            newPaths.forEach(path -> {
                var pathString = path.stream().map(AbstractNode::getID).collect(Collectors.joining("->"));
                if (unique.contains(pathString)) return;
                unique.add(pathString);
                criticalPaths.add(path);
            });
        });

        // 4. Calculate the risk reports based on the critical paths.
        List<RiskReport> riskReports = criticalPaths.stream().map(criticalPath -> {
            var path = criticalPath.stream().map(n -> (INode) n).toList();
            var graph = attackGraph.getGraphForPath(criticalPath);
            var report = RiskReport.fromCriticalPath(graph, path);
            return report;
        }).toList();

        return new ArrayList<>(riskReports);
    }

    public Consumer<RiskEdge> createRiskDispatcher(AttackGraph attackGraph) {
        return e -> {
            var from = attackGraph.findById(e.from().getID()).get();
            var to = attackGraph.findById(e.to().getID()).get();

            attackGraph.addEdge(from, to, e.risk());
        };
    }
}
