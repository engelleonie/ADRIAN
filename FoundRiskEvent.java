package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.risks.RiskReport;

public class FoundRiskEvent extends Event {
    private final RiskReport risk;

    public FoundRiskEvent(RiskReport risk, IAgent agent) {
        super(agent);
        this.risk = risk;
    }

    public RiskReport getRiskReport() {
        return risk;
    }

    @Override
    public int getDuration() {
        return 5;
    }

    @Override
    public boolean isDebugEvent() {
        return true;
    }
}
