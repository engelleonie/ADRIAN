package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.core.auction.Auction;
import tech.jorn.adrian.core.events.Event;

public class SearchForProposalEvent extends Event {
    private final Auction auction;

    public SearchForProposalEvent(Auction auction) {
        this.auction = auction;
    }

    public Auction getAuction() {
        return auction;
    }
}
