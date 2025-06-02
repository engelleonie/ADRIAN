package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.risks.RiskReport;

public class SelectedRiskEvent extends Event {
    private final RiskReport risk;

    public SelectedRiskEvent(RiskReport risk) {
        super();
        this.risk = risk;
    }

    public RiskReport getRiskReport() {
        return risk;
    }
}
