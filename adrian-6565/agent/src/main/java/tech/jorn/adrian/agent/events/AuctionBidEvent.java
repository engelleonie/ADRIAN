package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.core.auction.AuctionProposal;
import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.graphs.base.INode;

public class AuctionBidEvent extends Event {
    private final INode origin;
    private final AuctionProposal proposal;

    public AuctionBidEvent(INode origin, AuctionProposal proposal) {
        this.origin = origin;
        this.proposal = proposal;
    }

    public INode getOrigin() {
        return origin;
    }

    public AuctionProposal getProposal() {
        return proposal;
    }

    @Override
    public boolean isImmediate() {
        return true;
    }
}
