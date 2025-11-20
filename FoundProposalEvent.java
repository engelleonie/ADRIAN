package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.auction.AuctionProposal;
import tech.jorn.adrian.core.events.Event;

public class FoundProposalEvent extends Event {
    private final AuctionProposal p;

    public FoundProposalEvent(AuctionProposal p, IAgent agent) {
        super(agent);
        this.p = p;
    }

    public AuctionProposal getProposal() {
        return p;
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
