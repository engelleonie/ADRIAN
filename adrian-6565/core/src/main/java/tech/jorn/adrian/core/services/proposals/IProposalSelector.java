package tech.jorn.adrian.core.services.proposals;

import tech.jorn.adrian.core.auction.AuctionProposal;

import java.util.List;
import java.util.Optional;

public interface IProposalSelector {

    Optional<AuctionProposal> select(List<AuctionProposal> proposals, float threshold);
}
