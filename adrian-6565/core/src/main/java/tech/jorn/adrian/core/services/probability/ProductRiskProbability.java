package tech.jorn.adrian.core.services.probability;

import tech.jorn.adrian.core.risks.RiskEdge;

import java.util.List;

public class ProductRiskProbability implements IRiskProbabilityCalculator {
    @Override
    public float calculate(List<Float> risks) {
        return 1 - risks.stream()
                .reduce(1.0f, (acc, cur) -> acc * (1.0f - cur));
    }
}
