package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.core.auction.AuctionProposal;
import tech.jorn.adrian.core.events.Event;

public class ApplyProposalEvent extends Event {
    private final AuctionProposal proposal;

    public ApplyProposalEvent(AuctionProposal proposal) {
        this.proposal = proposal;
    }

    public AuctionProposal getProposal() {
        return proposal;
    }

    @Override
    public boolean isImmediate() {
        return true;
    }
}
