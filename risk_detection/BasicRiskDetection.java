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
import tech.jorn.adrian.core.properties.AbstractProperty;
import tech.jorn.adrian.core.risks.Risk;
import tech.jorn.adrian.core.risks.RiskEdge;
import tech.jorn.adrian.core.risks.RiskReport;
import tech.jorn.adrian.core.risks.RiskRule;
import tech.jorn.adrian.core.services.RiskDetection;
import tech.jorn.adrian.core.services.probability.IRiskProbabilityCalculator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
            else
                attackNode = AttackGraphSoftwareAsset.fromNode((KnowledgeBaseSoftwareAsset) node);

            node.getProperties().forEach(attackNode::setFromProperty);

            attackGraph.upsertNode(attackNode);
        });

        attackGraph.getNodes().stream()
                .filter(n -> !n.getID().equals(VoidNode.getIncoming().getID()))
                .forEach(n -> {

                    Risk defaultRisk = new Risk("default-exposure-risk", 1.0f, false, null);
                    attackGraph.addIncoming(n, defaultRisk);
                });


        attackGraph.getNodes().stream()
                .filter(n -> n instanceof AttackGraphSoftwareAsset)
                .forEach(n -> {
                    if (!(boolean) n.getProperty("isCritical").orElse(false)) {
                        n.setFromProperty("isCritical", new AbstractProperty<Boolean>("isCritical", true) {
                        });
                    }
                });


        Consumer<RiskEdge> dispatchRisk = this.createRiskDispatcher(attackGraph);
        this.riskRules.forEach(r -> r.evaluate(knowledgeBase, dispatchRisk));

        return attackGraph;
    }


    @Override
    public List<RiskReport> identifyRisks(AttackGraph attackGraph, boolean isContained) {

        var voidNode = attackGraph.findById(VoidNode.getIncoming().getID());
        List<? extends AttackGraphEntry<?>> exposed = attackGraph.getNeighbours(VoidNode.getIncoming());
        List<AttackGraphSoftwareAsset> criticalSoftware = new ArrayList<>();
        System.out.println(1111);
        //System.out.println("VoidNode neighbours: " + attackGraph.getNeighbours(VoidNode.getIncoming()));
        attackGraph.getNodes().forEach(node -> {
            //System.out.println("Node ID=" + node.getID() + ", class=" + node.getClass() + ", isCritical=" + node.getProperty("isCritical").orElse(false));
            if (node instanceof AttackGraphSoftwareAsset) {
                System.out.println(2222);
                if ((boolean) node.getProperty("isCritical").orElse(false)) {
                    criticalSoftware.add((AttackGraphSoftwareAsset) node);
                    System.out.println(3333);
                }
            }
        });


        System.out.println(exposed.isEmpty());
        System.out.println(criticalSoftware.isEmpty());
        if (exposed.isEmpty() || criticalSoftware.isEmpty()) {
            System.out.println(4444);
            return new ArrayList<>();
        }


        List<List<AttackGraphEntry<?>>> criticalPaths = new ArrayList<>();

        criticalSoftware.forEach(asset -> {
            var newPaths = attackGraph.findPathsTo(VoidNode.getIncoming(), asset).stream()
                    .filter(path -> {


                        if (this.configuration == null || !isContained) {
                            System.out.println(5555);
                            return true;
                        }
                        var contained = path.stream().filter(n -> n.getID().equals(this.configuration.getNodeID())).findAny();
                        System.out.println(6666);
                        return contained.isPresent();
                    })
                    .toList();
            var unique = new HashSet<String>();
            newPaths.forEach(path -> {
                var pathString = path.stream().map(AbstractNode::getID).collect(Collectors.joining("->"));
                if (unique.contains(pathString)) {
                    System.out.println(7777);
                    return;
                }
                unique.add(pathString);
                criticalPaths.add(path);
                System.out.println(8888);
            });
        });


        List<RiskReport> riskReports = criticalPaths.stream().map(criticalPath -> {
            var path = criticalPath.stream().map(n -> (INode) n).toList();
            var graph = attackGraph.getGraphForPath(criticalPath);
            var report = RiskReport.fromCriticalPath(graph, path);
            System.out.println(9999);
            return report;
        }).toList();
        System.out.println(0000);

        return new ArrayList<>(riskReports);
    }


    public Consumer<RiskEdge> createRiskDispatcher(AttackGraph attackGraph) {

        return e -> {
            //System.out.println("RiskEdge: from=" + e.from().getID() + " to=" + e.to().getID() + " risk=" + e.risk().toString());
            var from = attackGraph.findById(e.from().getID()).get();
            var to = attackGraph.findById(e.to().getID()).get();
            if (from == null || to == null) {
                System.out.println("Missing nodes in attack graph for edge.");
                return;
            }

            attackGraph.addEdge(from, to, e.risk());
        };
    }
}
