package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.auction.AuctionProposal;
import tech.jorn.adrian.core.events.Event;

public class ApplyProposalEvent extends Event {
    private final AuctionProposal proposal;


    public ApplyProposalEvent(AuctionProposal proposal, IAgent agent) {
        super(agent);
        this.proposal = proposal;
    }

    public AuctionProposal getProposal() {
        return proposal;
    }

    @Override
    public int getDuration() {
        return 0;
    }

    @Override
    public boolean isImmediate() {
        return true;
    }

}
