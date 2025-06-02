package tech.jorn.adrian.risks.rules;

import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBase;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBaseEntry;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBaseNode;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBaseSoftwareAsset;
import tech.jorn.adrian.core.risks.PropertyBasedRule;
import tech.jorn.adrian.core.risks.Risk;
import tech.jorn.adrian.core.risks.RiskEdge;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ForwardRisk extends PropertyBasedRule {
    public ForwardRisk(String ruleId, String property, float factor) {
        super(ruleId, property, factor);
    }

    @Override
    public void evaluate(KnowledgeBase knowledgeBase, Consumer<RiskEdge> dispatchRisk) {
        List<KnowledgeBaseEntry<?>> nodes = new ArrayList<>();
        knowledgeBase.getNodes().forEach(node -> {
            if (node.getID().equals("VOID")) return;
            if (this.nodesIncluded && node instanceof KnowledgeBaseNode) nodes.add(node);
            if (this.softwareIncluded && node instanceof KnowledgeBaseSoftwareAsset) nodes.add(node);
        });

        nodes.forEach(node -> {
            var isMitigated = this.isMitigated(node);

            // If it is mitigated, and we do not have a remaining risk to return, break
            if (isMitigated && this.mitigatedRiskSupplier == null) return;

            var siblings = knowledgeBase.getNeighbours(node);
            siblings.forEach(sibling -> {
                if (sibling.getID().equals(node.getID())) return;
                var risk = isMitigated
                        ? this.mitigatedRiskSupplier.get()
                        : new Risk(this.ruleId, this.factor, false, this);
                var edge = new RiskEdge(node, sibling, risk);
                dispatchRisk.accept(edge);
            });
        });
    }
}
