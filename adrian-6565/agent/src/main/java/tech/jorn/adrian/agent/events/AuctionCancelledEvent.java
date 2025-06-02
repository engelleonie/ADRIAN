package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.core.auction.Auction;
import tech.jorn.adrian.core.events.Event;

public class AuctionCancelledEvent extends Event {
    public AuctionCancelledEvent(Auction auction) {
    }

    @Override
    public boolean isImmediate() {
        return true;
    }
}
