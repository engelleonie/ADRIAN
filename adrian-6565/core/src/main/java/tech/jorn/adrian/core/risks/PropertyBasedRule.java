package tech.jorn.adrian.core.risks;

import tech.jorn.adrian.core.graphs.AbstractDetailedNode;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBaseEntry;
import tech.jorn.adrian.core.mutations.Mutation;
import tech.jorn.adrian.core.properties.AbstractProperty;
import tech.jorn.adrian.core.risks.validators.PropertyValidator;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class PropertyBasedRule extends RiskRule {

    protected final String ruleId;
    protected final String property;
    protected final float factor;
    protected boolean softwareIncluded = false;
    protected boolean nodesIncluded = true;
    protected boolean softwareTargeted = true;
    protected boolean nodesTargeted = true;
    protected Supplier<Risk> mitigatedRiskSupplier = null;
    protected Predicate<KnowledgeBaseEntry<?>> isMitigatedPred;
    protected BiFunction<AbstractDetailedNode, RiskRule, Optional<? extends Mutation>> adaptationFactory;

    protected PropertyBasedRule(String ruleId, String property, float factor) {
        this.property = property;
        this.ruleId = ruleId;
        this.factor = factor;
    }


    public String getProperty() {
        return property;
    }


    public PropertyBasedRule includeSoftware() {
        this.softwareIncluded = true;
        return this;
    }

    public PropertyBasedRule excludeNodes() {
        this.nodesIncluded = false;
        return this;
    }

    public PropertyBasedRule target(boolean nodes, boolean software) {
        this.nodesTargeted = nodes;
        this.softwareTargeted = software;
        return this;
    }

    public PropertyBasedRule mitigatedRisk(float damage) {
        this.mitigatedRiskSupplier = () -> new Risk(this.property, damage, true, this);
        return this;
    }

    public PropertyBasedRule mitigatedRisk(Supplier<Risk> riskSupplier) {
        this.mitigatedRiskSupplier = riskSupplier;
        return this;
    }

    public PropertyBasedRule mitigationTest(Predicate<KnowledgeBaseEntry<?>> test) {
        this.isMitigatedPred = test;
        return this;
    }

    public PropertyBasedRule mitigationTest(PropertyValidator<String> propertyValidator) {
        this.isMitigatedPred = node -> {
            var propertyValue = node.getProperty(this.property);
            if (propertyValue.isEmpty()) return true;
            return !propertyValidator.validate((String) propertyValue.get());
        };
        return this;
    }

    protected boolean isMitigated(KnowledgeBaseEntry<?> node) {
        if (this.isMitigatedPred == null) return this.isPropertyMitigated(node.getProperty(this.property));
        return this.isMitigatedPred.test(node);
    }

    private boolean isPropertyMitigated(Optional<?> property) {
        return property
                .map(p -> (Boolean) p)
                .orElse(Boolean.FALSE);
    }

    public PropertyBasedRule withAdaptation(BiFunction<AbstractDetailedNode, RiskRule, Optional<? extends Mutation>> factory) {
        this.adaptationFactory = factory;
        return this;
    }

    public <N extends AbstractDetailedNode<P>, P extends AbstractProperty<?>> Optional<Mutation<N>> getAdaptation(N node) {
        if (this.adaptationFactory == null) return Optional.empty();
        return (Optional<Mutation<N>>) this.adaptationFactory.apply(node, this);

        //        if ((node instanceof KnowledgeBaseSoftwareAsset) || (node instanceof AttackGraphSoftwareAsset)) return Optional.empty();
        //        System.out.println("Checking mitigation for " + node.getID() + " with property " + this.getProperty() + " and value " + node.getProperty(this.getProperty()) + " mitigated " +this.isMitigated(node.getProperty(this.getProperty())));
        //        if (this.isMitigated(node.getProperty(this.getProperty()))) return Optional.empty();
        //
        //        var adaptation = new AttributeChange<>(node, new NodeProperty<>(this.getProperty(), true), 100, 2000, this);
        //        return Optional.of(adaptation);
    }
}
