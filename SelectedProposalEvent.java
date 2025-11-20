package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.auction.AuctionProposal;
import tech.jorn.adrian.core.events.Event;

public class SelectedProposalEvent extends Event {
    private final AuctionProposal proposal;

    public SelectedProposalEvent(AuctionProposal proposal, IAgent agent) {
        super(agent);
        this.proposal = proposal;
    }

    @Override
    public int getDuration() {
        return 10;
    }

    public AuctionProposal getProposal() {
        return proposal;
    }
}
