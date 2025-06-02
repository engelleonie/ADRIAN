package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.core.auction.Auction;
import tech.jorn.adrian.core.auction.AuctionProposal;
import tech.jorn.adrian.core.events.Event;

public class AuctionFinalizedEvent extends Event {
    private final Auction auction;
    private final AuctionProposal proposal;

    public AuctionFinalizedEvent(Auction auction, AuctionProposal proposal) {
        this.auction = auction;
        this.proposal = proposal;
    }

    public Auction getAuction() {
        return auction;
    }

    public AuctionProposal getProposal() {
        return proposal;
    }

    @Override
    public boolean isImmediate() {
        return true;
    }
}
