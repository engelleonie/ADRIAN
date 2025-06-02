package tech.jorn.adrian.core.auction;

import tech.jorn.adrian.core.graphs.base.INode;
import tech.jorn.adrian.core.mutations.Mutation;
import tech.jorn.adrian.core.risks.RiskReport;

public record AuctionProposal(INode origin, Auction auction, Mutation mutation, RiskReport updatedReport) {
}
