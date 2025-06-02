package tech.jorn.adrian.core.risks;

import tech.jorn.adrian.core.graphs.base.INode;
import tech.jorn.adrian.core.graphs.risks.AttackGraph;
import tech.jorn.adrian.core.graphs.risks.AttackGraphEntry;
import tech.jorn.adrian.core.graphs.risks.AttackGraphLink;
import tech.jorn.adrian.core.graphs.risks.AttackGraphSoftwareAsset;
import tech.jorn.adrian.core.services.probability.ProductRiskProbability;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @param graph Attack graph
 * @param path Path taken through the graph
 * @param probability Probability of the path
 * @param damageValue Damage value of the path
 * @param damage Calculated damage of the path
 */
public record RiskReport(AttackGraph graph, List<? extends INode> path, float probability, float damageValue, float damage) {

    // public String toString() {
    //     var path = this.path.stream().map(INode::getID).collect(Collectors.joining("->"));
    //     // return String.format("%s %.2f %.2f %.2f", path, this.probability, this.damageValue, this.damage);
    //     return String.format("%s %.2f", path, this.damage);
    // }
    public String toString() {
        var calculator = new ProductRiskProbability();
        // var path = this.path.stream().map(INode::getID).collect(Collectors.joining("->"));
        var attackPath = this.graph.getGraphForPath((List<AttackGraphEntry<?>>) (Object) this.path());
        String pathString = path.stream().map(n -> {
            if (n instanceof AttackGraphSoftwareAsset) return n.getID();

            var attackNode = attackPath.findById(n.getID()).get();
            List<Float> risks = attackPath.getNeighboursWithRisks(attackNode).stream()
                .map(l -> l.getRisk().factor())
                .collect(Collectors.toList());
            var probability = calculator.calculate(risks);
            return String.format(" %s -[%.2f]->", attackNode.getID(), probability);
        }).collect(Collectors.joining(" "));
        // return String.format("%s %.2f %.2f %.2f", path, this.probability, this.damageValue, this.damage);
        return String.format("%s %.2f %.2f", pathString, this.damage, this.probability);
    }

    public String toShortString() {
        return path.stream().map(INode::getID).collect(Collectors.joining("->"));
    }

    public static RiskReport fromCriticalPath(AttackGraph attackGraph, List<? extends INode> criticalPath) {
        var softwareId = criticalPath.get(criticalPath.size()-1).getID();
        var software = attackGraph.findById(softwareId).get();
        var damage = (float) software.getProperty("damageValue").orElse(0.0f);

        var compactGraph = attackGraph.getGraphForPath((List<AttackGraphEntry<?>>) criticalPath);
        var probability = attackGraph.getProbabilityForPath((List<AttackGraphEntry<?>>) criticalPath, new ProductRiskProbability());
        return new RiskReport(compactGraph, criticalPath, probability, damage, probability * damage);
    }
}
