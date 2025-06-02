package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.risks.RiskReport;

public class InitiateAuctionEvent extends Event {
    private final RiskReport report;

    public InitiateAuctionEvent(RiskReport report) {
        this.report = report;
    }

    public RiskReport getReport() {
        return report;
    }
}
