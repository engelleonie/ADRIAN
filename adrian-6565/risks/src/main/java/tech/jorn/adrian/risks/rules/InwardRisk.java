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

public class InwardRisk extends PropertyBasedRule {


    public InwardRisk(String ruleId, String property, float factor) {
        super(ruleId, property, factor);
    }

    @Override
    public void evaluate(KnowledgeBase knowledgeBase, Consumer<RiskEdge> dispatchRisk) {
        var nodes = knowledgeBase.getNeighbours(VoidNode.forKnowledge());

        nodes.forEach(node -> {
            var isMitigated = this.property != null && this.isMitigated(node);

            // If it is mitigated, and we do not have a remaining risk to return, break
            if (isMitigated && this.mitigatedRiskSupplier == null) return;

            var exposed = this.property == null && (boolean) node.getProperty("exposed").orElse(false);
            if (!exposed) return;

            var risk = isMitigated
                    ? this.mitigatedRiskSupplier.get()
                    : new Risk(this.ruleId, this.factor, false, this);
            var edge = new RiskEdge(VoidNode.forKnowledge(), node, risk);
            dispatchRisk.accept(edge);
        });
    }
}