package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.risks.RiskReport;

public class SelectedRiskEvent extends Event {
    private final RiskReport risk;


    public SelectedRiskEvent(RiskReport risk, IAgent agent) {
        super(agent);
        this.risk = risk;
    }

    @Override
    public int getDuration() {
        return 10;
    }

    public RiskReport getRiskReport() {
        return risk;
    }
}
