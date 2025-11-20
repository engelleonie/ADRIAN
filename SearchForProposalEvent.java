package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.auction.Auction;
import tech.jorn.adrian.core.events.Event;

public class SearchForProposalEvent extends Event {
    private final Auction auction;

    public SearchForProposalEvent(Auction auction, IAgent agent) {
        super(agent);
        this.auction = auction;
    }

    @Override
    public int getDuration() {
        return 10;
    }

    public Auction getAuction() {
        return auction;
    }
}
