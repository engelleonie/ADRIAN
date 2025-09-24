package tech.jorn.adrian.core.services.proposals;

import java.util.Set;

public interface ProposalListener {
    void onProposalApplied(Set<String> changedNodeIds);
}