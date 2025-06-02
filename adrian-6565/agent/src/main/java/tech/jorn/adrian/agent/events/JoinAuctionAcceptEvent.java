package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.core.auction.Auction;
import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.graphs.base.INode;

public class JoinAuctionAcceptEvent extends Event {
    private final INode origin;
    private final Auction auction;

    public JoinAuctionAcceptEvent(INode origin, Auction auction) {
        this.origin = origin;
        this.auction = auction;
    }

    public INode getOrigin() {
        return origin;
    }

    public Auction getAuction() {
        return auction;
    }
}
