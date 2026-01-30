package tech.jorn.adrian.agent.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.jorn.adrian.agent.AdrianAgent;
import tech.jorn.adrian.agent.events.*;
import tech.jorn.adrian.core.GlobalQueue;
import tech.jorn.adrian.core.agents.AgentState;
import tech.jorn.adrian.core.controllers.AbstractController;
import tech.jorn.adrian.core.events.EventManager;
import tech.jorn.adrian.core.observables.SubscribableValueEvent;
import tech.jorn.adrian.core.services.proposals.ProposalManager;

import java.util.Random;
import java.util.random.*;

public class ProposalController extends AbstractController {
    Logger log = LogManager.getLogger(ProposalController.class);

    private final ProposalManager proposalManager;
    Random rand = new Random();

    public ProposalController(ProposalManager proposalManager, EventManager eventManager, SubscribableValueEvent<AgentState> agentState) {
        super(eventManager, agentState);

        this.proposalManager = proposalManager;

        //handlers for search and application
        this.eventManager.registerEventHandler(SearchForProposalEvent.class, this::searchForProposal);
        this.eventManager.registerEventHandler(ApplyProposalEvent.class, this::applyProposal);
    }

    protected void searchForProposal(SearchForProposalEvent event) {
        var proposals = this.proposalManager.findProposals(event.getAuction());
        var proposal = this.proposalManager.selectProposal(proposals, event.getAuction());

        //found proposals contain all possible changes
        proposals.forEach(p -> eventManager.emit(new FoundProposalEvent(p, event.getAgent())));
        // selects the best proposal
        proposal.ifPresentOrElse(
                p -> {
                    eventManager.emit(new SelectedProposalEvent(p, event.getAgent()));
                },
                () -> {
                    this.log.warn("No proposal was found with the given constrains, tried {} proposals", proposals.size());
                    eventManager.emit(new CancelProposalEvent(event.getAuction(), event.getAgent()));
                }
        );
    }

    protected void applyProposal(ApplyProposalEvent event) {
        // applies proposal with the highest damage reduction
        this.log.info("Applying proposal from auction {}: {}", event.getProposal().auction().getId(), event.getProposal().mutation().toString());
        var changed = proposalManager.applyProposal(event.getProposal());

        // triggers new risk identification after randomized delay to avoid all agents searching for and mitigating their risks at the same time
            GlobalQueue.getInstance().offer(new IdentifyRiskEvent(event.getAgent()), rand.nextInt(50));


         //eventManager.getQueue().clear(); // Maybe remove this

    }
}

