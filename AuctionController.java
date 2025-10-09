package tech.jorn.adrian.agent.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.jorn.adrian.agent.events.*;
import tech.jorn.adrian.agent.services.AuctionManager;
import tech.jorn.adrian.core.agents.AgentState;
import tech.jorn.adrian.core.agents.IAgentConfiguration;
import tech.jorn.adrian.core.controllers.AbstractController;
import tech.jorn.adrian.agent.events.AuctionFinalizedEvent;
import tech.jorn.adrian.core.events.EventManager;
import tech.jorn.adrian.core.mutations.Migration;
import tech.jorn.adrian.core.observables.ValueDispatcher;

public class AuctionController extends AbstractController {
    Logger log = LogManager.getLogger(AuctionController.class);
    private final AuctionManager auctionManager;
    private final IAgentConfiguration configuration;

    public AuctionController(AuctionManager auctionManager, EventManager eventManager, IAgentConfiguration configuration, ValueDispatcher<AgentState> agentState) {
        super(eventManager, agentState.subscribable);
        this.auctionManager = auctionManager;
        this.configuration = configuration;
        this.registerEvents();

        this.auctionManager.onAuctionChanged().subscribe(auction ->  {
            if (auction == null) {
                if (agentState.current().equals(AgentState.Auctioning))
                    agentState.setCurrent(AgentState.Idle);
                else this.log.warn("Auction stopped but agent was not auctioning");
            }
            else agentState.setCurrent(AgentState.Auctioning);
        });
    }

    private void registerEvents() {
        this.eventManager.registerEventHandler(InitiateAuctionEvent.class, this::initiateAuction);
        this.eventManager.registerEventHandler(JoinAuctionRequestEvent.class, this::joinAuctionRequest);
        this.eventManager.registerEventHandler(JoinAuctionAcceptEvent.class, this::joinAuctionAccept);
        this.eventManager.registerEventHandler(JoinAuctionRejectEvent.class, this::joinAuctionReject);

        this.eventManager.registerEventHandler(AuctionBidEvent.class, this::receiveAuctionBid);
        this.eventManager.registerEventHandler(SelectedProposalEvent.class, this::selectedProposal);
        this.eventManager.registerEventHandler(CancelProposalEvent.class, this::cancelProposal);
        this.eventManager.registerEventHandler(AuctionFinalizedEvent.class, this::onAuctionFinalized);
        this.eventManager.registerEventHandler(AuctionCancelledEvent.class, this::onAuctionCancelled);
    }

    private void initiateAuction(InitiateAuctionEvent event) {
        if (this.auctionManager.isAuctioning()) {
            this.log.warn("Agent was already participating in an auction");
            return;
        }
        this.auctionManager.startAuction(event.getReport());
    }

    private void joinAuctionRequest(JoinAuctionRequestEvent event) {
        boolean canJoin = !this.auctionManager.isAuctioning() && (this.agentState.current() == AgentState.Idle || this.agentState.current() == AgentState.Ready);
        if (canJoin) this.auctionManager.joinAuction(event.getAuction());
        else this.auctionManager.rejectAuction(event.getAuction());
    }

    private void joinAuctionAccept(JoinAuctionAcceptEvent event) {
        this.auctionManager.onAuctionJoined(event.getAuction(), event.getOrigin());
    }

    private void joinAuctionReject(JoinAuctionRejectEvent event) {
        this.auctionManager.onAuctionRejected(event.getAuction(), event.getOrigin());
    }

    private void receiveAuctionBid(AuctionBidEvent event) {
        this.auctionManager.receiveProposal(event.getProposal(), event.getOrigin());
        this.eventManager.emit(new FoundProposalEvent(event.getProposal()));
    }

    private void selectedProposal(SelectedProposalEvent event) {
        this.auctionManager.bidProposal(event.getProposal());
    }
    private void cancelProposal(CancelProposalEvent event) {
        this.auctionManager.cancelProposal(event.getAuction());
    }

    private void onAuctionFinalized(AuctionFinalizedEvent event) {
        System.out.println("auction finalized");
        // TODO: Remove auction from AuctionManager so we are able to join another auction
        this.auctionManager.reset();
        // If we are not the one on the proposal ignore it
        boolean shouldApply = false;

        var proposal = event.getProposal();
        var mutation = proposal.mutation();
        if ((proposal.origin().equals(this.configuration.getParentNode()))) {
            shouldApply = true;
            System.out.println("Origin matches parent node");
        }
        if (mutation instanceof Migration<?,?> && ((Migration<?, ?>) mutation).getFrom().getID().equals(this.configuration.getNodeID()))  {
            shouldApply = true;
            System.out.println("mutation 1");
        }
        if (mutation instanceof Migration<?,?> && ((Migration<?, ?>) mutation).getNode().getID().equals(this.configuration.getNodeID())) {
            shouldApply = true;
            System.out.println("mutation 2");
        }

        if (shouldApply) {
            this.eventManager.emit(new ApplyProposalEvent(event.getProposal()));
            System.out.println("Proposal applied");

        }
    }
    private void onAuctionCancelled(AuctionCancelledEvent e) {
        this.auctionManager.reset();
    }
}
