package tech.jorn.adrian.core.services.probability;

import tech.jorn.adrian.core.risks.RiskEdge;

import java.util.List;

public interface IRiskProbabilityCalculator {
    float calculate(List<Float> risks);
}
