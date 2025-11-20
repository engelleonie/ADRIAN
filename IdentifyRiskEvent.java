package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.agent.AdrianAgent;
import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.events.Event;

public class IdentifyRiskEvent extends Event {
    private String agentID;
    private boolean due;

    public IdentifyRiskEvent(IAgent agent) {
        super(agent);
        this.agentID = agent.getID();
    }

    public String getAgentID() {
        return agentID;
    }

    public boolean isDue() { return due; }
    public void markDue() { this.due = true; }

    @Override
    public int getDuration() { return 50; }









}
