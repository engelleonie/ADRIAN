package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.auction.Auction;
import tech.jorn.adrian.core.events.Event;

public class AuctionCancelledEvent extends Event {
    public AuctionCancelledEvent(Auction auction, IAgent agent) {
        super(agent);

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
