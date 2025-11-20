package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.auction.Auction;
import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.graphs.base.INode;

public class JoinAuctionRejectEvent extends Event {
    private final INode origin;
    private final Auction auction;

    public JoinAuctionRejectEvent(INode origin, Auction auction, IAgent agent) {
        super(agent);
        this.origin = origin;
        this.auction = auction;
    }

    @Override
    public int getDuration() {
        return 10;
    }

    public INode getOrigin() {
        return origin;
    }

    public Auction getAuction() {
        return auction;
    }
}
