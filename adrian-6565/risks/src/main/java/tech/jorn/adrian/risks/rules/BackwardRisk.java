package tech.jorn.adrian.risks.rules;

import tech.jorn.adrian.core.graphs.AbstractDetailedNode;
import tech.jorn.adrian.core.graphs.base.VoidNode;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBase;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBaseEntry;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBaseNode;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBaseSoftwareAsset;
import tech.jorn.adrian.core.mutations.Mutation;
import tech.jorn.adrian.core.properties.AbstractProperty;
import tech.jorn.adrian.core.risks.PropertyBasedRule;
import tech.jorn.adrian.core.risks.Risk;
import tech.jorn.adrian.core.risks.RiskEdge;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class BackwardRisk  extends PropertyBasedRule {


    public BackwardRisk(String ruleId, String property, float factor) {
        super(ruleId, property, factor);
    }

    @Override
    public void evaluate(KnowledgeBase knowledgeBase, Consumer<RiskEdge> dispatchRisk) {
        List<KnowledgeBaseEntry<?>> nodes = new ArrayList<>();
        knowledgeBase.getNodes().forEach(node -> {
            if (this.nodesIncluded && node instanceof KnowledgeBaseNode) nodes.add(node);
            if (this.softwareIncluded && node instanceof KnowledgeBaseSoftwareAsset) nodes.add(node);
        });

        nodes.forEach(node -> {
            var isMitigated = this.isMitigated(node);
            var exposed = (boolean) node.getProperty("exposed").orElse(false);

            // If it is mitigated, and we do not have a remaining risk to return, break
            if (isMitigated && this.mitigatedRiskSupplier == null) return;

            var parents = knowledgeBase.getParents(node);
            parents.forEach(parent -> {

                if (parent.getID().equals(node.getID())) return;
                if (!nodesTargeted && parent instanceof KnowledgeBaseNode) return;
                if (!softwareTargeted && parent instanceof KnowledgeBaseSoftwareAsset) return;
                if (!exposed && parent.getID().equals(VoidNode.forKnowledge().getID())) return;

                var risk = isMitigated
                        ? this.mitigatedRiskSupplier.get()
                        : new Risk(this.ruleId, this.factor, false, this);
                var edge = new RiskEdge(parent, node, risk);
                dispatchRisk.accept(edge);
            });
        });
    }
}