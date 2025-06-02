package tech.jorn.adrian.core.risks;

import tech.jorn.adrian.core.graphs.AbstractDetailedNode;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBaseEntry;
import tech.jorn.adrian.core.mutations.Mutation;
import tech.jorn.adrian.core.properties.AbstractProperty;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class RiskType {
    private final String label;
    private final Float unmitigatedRiskFactor;
    private final Float mitigatedRiskFactor;

    public RiskType(String label, Float unmitigatedRiskFactor, Float mitigatedRiskFactor) {
        this.label = label;
        this.unmitigatedRiskFactor = unmitigatedRiskFactor;
        this.mitigatedRiskFactor = mitigatedRiskFactor;
    }

    public String getLabel() {
        return label;
    }

    public Float getUnmitigatedRiskFactor() {
        return unmitigatedRiskFactor;
    }

    public Float getMitigatedRiskFactor() {
        return mitigatedRiskFactor;
    }
}
