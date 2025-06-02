package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.core.auction.Auction;
import tech.jorn.adrian.core.events.Event;

public class JoinAuctionRequestEvent extends Event {
    private final Auction auction;

    public JoinAuctionRequestEvent(Auction auction) {
        super();
        this.auction = auction;
    }

    public Auction getAuction() {
        return auction;
    }

    @Override
    public boolean isImmediate() {
        return true;
    }
}
