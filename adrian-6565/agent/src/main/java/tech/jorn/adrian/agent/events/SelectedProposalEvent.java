package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.core.auction.AuctionProposal;
import tech.jorn.adrian.core.events.Event;

import java.util.Optional;

public class SelectedProposalEvent extends Event {
    private final AuctionProposal proposal;

    public SelectedProposalEvent(AuctionProposal proposal) {
        this.proposal = proposal;
    }

    public AuctionProposal getProposal() {
        return proposal;
    }
}
