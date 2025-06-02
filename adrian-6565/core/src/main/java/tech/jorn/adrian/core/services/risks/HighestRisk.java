package tech.jorn.adrian.core.services.risks;

import tech.jorn.adrian.core.risks.RiskReport;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class HighestRisk implements IRiskSelector {

    private final float threshold;

    public HighestRisk(float threshold) {
        this.threshold = threshold;
    }

    @Override
    public Optional<RiskReport> select(List<RiskReport> risks) {
        return risks.stream()
                .filter(report -> report.damage() > this.threshold)
                .max(Comparator.comparingDouble(RiskReport::damage));
    }
}
