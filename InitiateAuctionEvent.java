package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.risks.RiskReport;

public class InitiateAuctionEvent extends Event {
    private final RiskReport report;

    public InitiateAuctionEvent(RiskReport report, IAgent agent) {
        super(agent);
        this.report = report;
    }

    @Override
    public int getDuration() {
        return 10;
    }

    public RiskReport getReport() {
        return report;
    }
}
