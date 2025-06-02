package tech.jorn.adrian.core.auction;

import tech.jorn.adrian.core.graphs.base.INode;
import tech.jorn.adrian.core.risks.RiskReport;

import java.util.Date;
import java.util.List;

public class Auction {
    private final String id;
    private final Date startDate;
    private final INode host;
    private final List<String> participants;
    private final RiskReport riskReport;

    public Auction(String id, INode host, List<String> participants, RiskReport riskReport) {
        this.id = id;
        this.host = host;
        this.participants = participants;
        this.riskReport = riskReport;
        this.startDate = new Date();
    }

    public String getId() {
        return id;
    }

    public Date getStartDate() {
        return startDate;
    }

    public INode getHost() {
        return host;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public RiskReport getRiskReport() {
        return riskReport;
    }
}

