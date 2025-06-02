package tech.jorn.adrian.experiment.instruments;

import tech.jorn.adrian.agent.events.*;
import tech.jorn.adrian.core.agents.AgentState;
import tech.jorn.adrian.core.agents.IAgentConfiguration;
import tech.jorn.adrian.core.auction.Auction;
import tech.jorn.adrian.core.controllers.AbstractController;
import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.events.EventManager;
import tech.jorn.adrian.core.observables.SubscribableValueEvent;
import tech.jorn.adrian.core.services.IDGenerator;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProposalImplementationController extends AbstractController {

    private Logger log;
    private final IAgentConfiguration configuration;

    public ProposalImplementationController(EventManager eventManager, IAgentConfiguration configuration, SubscribableValueEvent<AgentState> agentState) {
        super(eventManager, agentState);
        this.configuration = configuration;

        this.log = LogManager.getLogger(String.format("[%s] %s", configuration.getNodeID(), ProposalImplementationController.class.getSimpleName()));

        this.registerEvents();
    }

    private void registerEvents() {
        this.eventManager.registerEventHandler(JoinAuctionRequestEvent.class, this::doNothing);
        this.eventManager.registerEventHandler(JoinAuctionAcceptEvent.class, this::doNothing);
        this.eventManager.registerEventHandler(JoinAuctionRejectEvent.class, this::doNothing);

        this.eventManager.registerEventHandler(AuctionBidEvent.class, this::doNothing);
        this.eventManager.registerEventHandler(CancelProposalEvent.class, this::doNothing);
        this.eventManager.registerEventHandler(AuctionFinalizedEvent.class, this::doNothing);
        this.eventManager.registerEventHandler(AuctionCancelledEvent.class, this::doNothing);

        this.eventManager.registerEventHandler(InitiateAuctionEvent.class, this::initiateAuction);
        this.eventManager.registerEventHandler(SelectedProposalEvent.class, this::selectedProposal);
    }

    public void doNothing(Event e) {
    }

    public void initiateAuction(InitiateAuctionEvent event) {
        var auction = new Auction(new IDGenerator().getID(), this.configuration.getParentNode(), new ArrayList<>(), event.getReport());
        this.eventManager.emit(new SearchForProposalEvent(auction));
    }
    public void selectedProposal(SelectedProposalEvent event) {
        this.log.info("Selected proposal that will reduce the damage to {}: {}", event.getProposal().updatedReport().damage(), event.getProposal().mutation().toString());
        this.eventManager.emit(new ApplyProposalEvent(event.getProposal()));
    }
}
