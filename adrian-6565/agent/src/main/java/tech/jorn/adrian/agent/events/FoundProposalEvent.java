package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.core.auction.AuctionProposal;
import tech.jorn.adrian.core.events.Event;

public class FoundProposalEvent extends Event {
    private final AuctionProposal p;

    public FoundProposalEvent(AuctionProposal p) {
        this.p = p;
    }

    public AuctionProposal getProposal() {
        return p;
    }

    @Override
    public boolean isImmediate() {
        return true;
    }

}
