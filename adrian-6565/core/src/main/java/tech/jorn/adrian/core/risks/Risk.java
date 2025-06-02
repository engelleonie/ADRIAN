package tech.jorn.adrian.core.risks;

public record Risk(String type, float factor, boolean isMitigated, RiskRule rule) {
}
