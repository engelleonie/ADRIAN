package tech.jorn.adrian.core.services.risks;

import tech.jorn.adrian.core.risks.RiskReport;

import java.util.List;
import java.util.Optional;

public interface IRiskSelector {
    Optional<RiskReport> select(List<RiskReport> risks);
}
