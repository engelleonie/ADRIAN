package tech.jorn.adrian.agent.controllers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.jorn.adrian.agent.events.*;
import tech.jorn.adrian.core.agents.AgentState;
import tech.jorn.adrian.core.controllers.AbstractController;
import tech.jorn.adrian.core.controllers.AbstractEventController;
import tech.jorn.adrian.core.events.EventManager;
import tech.jorn.adrian.core.graphs.AbstractDetailedNode;
import tech.jorn.adrian.core.observables.SubscribableValueEvent;
import tech.jorn.adrian.core.properties.AbstractProperty;
import tech.jorn.adrian.core.services.proposals.ProposalManager;

import java.io.FileWriter;
import java.io.IOException;

public class ProposalController extends AbstractController {
    Logger log = LogManager.getLogger(ProposalController.class);

    private final ProposalManager proposalManager;

    public ProposalController(ProposalManager proposalManager, EventManager eventManager, SubscribableValueEvent<AgentState> agentState) {
        super(eventManager, agentState);

        this.proposalManager = proposalManager;

        // TODO: Probably also implement some event for selecting proposals?
        this.eventManager.registerEventHandler(SearchForProposalEvent.class, this::searchForProposal);
        this.eventManager.registerEventHandler(ApplyProposalEvent.class, this::applyProposal);
    }

    protected void searchForProposal(SearchForProposalEvent event) {
        var proposals = this.proposalManager.findProposals(event.getAuction());
        var proposal = this.proposalManager.selectProposal(proposals, event.getAuction());

        proposals.forEach(p -> eventManager.emit(new FoundProposalEvent(p)));
        proposal.ifPresentOrElse(
                p -> {
                    eventManager.emit(new SelectedProposalEvent(p));
                },
                () -> {
                    this.log.warn("No proposal was found with the given constrains, tried {} proposals", proposals.size());
                    eventManager.emit(new CancelProposalEvent(event.getAuction()));
                }
        );
    }

    protected void applyProposal(ApplyProposalEvent event) {
        this.log.info("Applying proposal from auction {}: {}", event.getProposal().auction().getId(), event.getProposal().mutation().toString());
        proposalManager.applyProposal(event.getProposal());
         eventManager.getQueue().clear(); // Maybe remove this

    }
}

