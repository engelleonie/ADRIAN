package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.auction.Auction;
import tech.jorn.adrian.core.events.Event;

public class CancelProposalEvent extends Event {
    private final Auction auction;

    public CancelProposalEvent(Auction auction, IAgent agent) {
        super(agent);
        this.auction = auction;
    }

    public Auction getAuction() {
        return auction;
    }

    @Override
    public int getDuration() {
        return 10;
    }

    @Override
    public boolean isImmediate() {
        return true;
    }
}
