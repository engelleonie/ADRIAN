package tech.jorn.adrian.agent.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.jorn.adrian.agent.controllers.AuctionController;
import tech.jorn.adrian.agent.events.*;
import tech.jorn.adrian.core.agents.IAgentConfiguration;
import tech.jorn.adrian.core.auction.Auction;
import tech.jorn.adrian.core.auction.AuctionProposal;
import tech.jorn.adrian.core.events.EventManager;
import tech.jorn.adrian.core.graphs.base.INode;
import tech.jorn.adrian.core.graphs.base.VoidNode;
import tech.jorn.adrian.core.graphs.risks.AttackGraphNode;
import tech.jorn.adrian.core.messages.EventMessage;
import tech.jorn.adrian.core.messages.MessageBroker;
import tech.jorn.adrian.core.observables.SubscribableValueEvent;
import tech.jorn.adrian.core.observables.ValueDispatcher;
import tech.jorn.adrian.core.risks.RiskReport;
import tech.jorn.adrian.core.services.IDGenerator;
import tech.jorn.adrian.core.services.proposals.IProposalSelector;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AuctionManager {
    Logger log;

    private final MessageBroker messageBroker;
    private final EventManager eventManager;
    private final IProposalSelector proposalSelector;
    private final IAgentConfiguration configuration;
    private TimerTask timeout;
    private Map<INode, Optional<AuctionProposal>> proposals = new HashMap<>();
    private Set<INode> participants = ConcurrentHashMap.newKeySet();
    private Set<INode> confirmed = ConcurrentHashMap.newKeySet();

    public AuctionManager(MessageBroker messageBroker, EventManager eventManager, IProposalSelector proposalSelector,
            IAgentConfiguration configuration) {
        this.messageBroker = messageBroker;
        this.eventManager = eventManager;
        this.proposalSelector = proposalSelector;
        this.configuration = configuration;

        this.log = LogManager
                .getLogger(String.format("[%s] %s", configuration.getNodeID(), AuctionManager.class.getSimpleName()));

    }

    private final ValueDispatcher<Auction> auction = new ValueDispatcher<>(null);

    public Auction startAuction(RiskReport riskReport) {
        try {
            Thread.sleep((int) Math.floor(Math.random() * 225) + 25);
        } catch (Exception e) {
            this.log.error("Could not sleep");
        }
        if (this.auction.current() != null) {
            this.log.warn("Started or joined another auction while in a grace period");
            return null;
        }

        var auction = new Auction(new IDGenerator().getID(), this.configuration.getParentNode(), new ArrayList<>(),
                riskReport);
        this.log.info("-- Auction Started!! {} damage {}", auction.getId(), auction.getRiskReport().damage());

        var timer = new Timer();
        var manager = this;
        if (this.timeout != null)
            this.timeout.cancel();
        this.timeout = new TimerTask() {
            @Override
            public void run() {
                manager.log.error("Auction {} timed out, received {}/{} proposals", auction.getId(),
                        manager.proposals.size(), manager.participants.size());
                manager.finalizeAuction(auction);
            }
        };
        timer.schedule(this.timeout, this.configuration.getAuctionTimeout());

        riskReport.graph().getNodes().forEach(node -> {
            if (!(node instanceof AttackGraphNode))
                return;
            if (node.getID().equals(this.configuration.getNodeID()))
                return;
            if (node.getID().equals(VoidNode.getIncoming().getID()))
                return;
            this.messageBroker.send(node, new EventMessage<>(new JoinAuctionRequestEvent(auction)));
            this.participants.add(node);
        });

        // Invite neighbours to the auction
        var inviteNeighbours = true;
        if (inviteNeighbours && riskReport.path().size() <= 5) {
            var additionalNodes = new ArrayList<String>();
            this.configuration.getNeighbours().forEach(node -> {
                var tempNode = new AttackGraphNode(node);
                if (this.participants.contains(tempNode)) return;

                this.messageBroker.send(tempNode, new EventMessage<>(new JoinAuctionRequestEvent(auction)));
                this.participants.add(tempNode);
                additionalNodes.add(node);
            });
            this.log.debug("Added {} neighbouring nodes {}", additionalNodes.size(), String.join(", ", additionalNodes));
        }

        this.auction.setCurrent(auction);
        this.eventManager.emit(new SearchForProposalEvent(auction));
        this.participants.add(this.configuration.getParentNode());
        this.confirmed.add(this.configuration.getParentNode());
        return auction;
    }

    public void joinAuction(Auction auction) {
        var event = new JoinAuctionAcceptEvent(this.configuration.getParentNode(), auction);
        this.messageBroker.send(auction.getHost(), new EventMessage<>(event));

        this.auction.setCurrent(auction);
        this.eventManager.emit(new SearchForProposalEvent(auction));
    }

    public void rejectAuction(Auction auction) {
        var event = new JoinAuctionRejectEvent(this.configuration.getParentNode(), auction);
        this.messageBroker.send(auction.getHost(), new EventMessage<>(event));
    }

    public void receiveProposal(AuctionProposal proposal, INode participant) {
        var auction = this.auction.current();
        if (auction == null) {
            this.log.warn("Received proposal for unknown auction");
            return;
        }
        if (!auction.getId().equals(proposal.auction().getId())) {
            this.log.warn("Should not have received a proposal when not the auctioneer node");
            return;
        }

        if (proposal.mutation() == null) {
            this.log.info("Node {} did not send a proposal", participant.getID());
        } else {
            var report = proposal.updatedReport();
            if (report == null) {
                this.log.warn("Proposal from {} has null updatedReport!", participant.getID());
                return;
            } else {
                this.log.info("Received proposal from {} which reduces damage to {}", participant.getID(), report.damage());
            }
        }

        var node = this.proposals.keySet().stream().filter(n -> n.getID().equals(participant.getID())).findFirst();
        if (node.isEmpty()) this.proposals.put(participant, Optional.ofNullable(proposal));
        else {
            this.proposals.put(node.get(), Optional.ofNullable(proposal));
        }

        if (this.isSaturated()) {
            this.finalizeAuction(auction);
        } else {
            var _participants = this.participants.stream()
                    .map(n -> n.getID())
                    .collect(Collectors.joining(", "));
            var _proposals = this.proposals.keySet().stream()
                    .map(n -> n.getID())
                    .collect(Collectors.joining(", "));
            var _confirmed = this.confirmed.stream()
                    .map(n -> n.getID())
                    .collect(Collectors.joining(", "));
            this.log.debug("\n{}\n{}\n{}\n", _participants, _proposals, _confirmed);
            this.log.debug("Received {} proposals, waiting for {} more (confirmed: {})", this.proposals.size(),
                    this.participants.size() - this.proposals.size(), this.confirmed.size());
        }
    }

    public void bidProposal(AuctionProposal proposal) {
        var event = new AuctionBidEvent(this.configuration.getParentNode(), proposal);
        this.messageBroker.send(proposal.auction().getHost(), new EventMessage<>(event));
    }

    public void cancelProposal(Auction auction) {
        var event = new AuctionBidEvent(this.configuration.getParentNode(), new AuctionProposal(
                this.configuration.getParentNode(), auction, null, null));
        this.messageBroker.send(auction.getHost(), new EventMessage<>(event));
    }

    public void onAuctionJoined(Auction auction, INode participant) {
        this.log.debug("Agent {} joined auction", participant.getID());

        var node = this.proposals.keySet().stream().filter(n -> n.getID().equals(participant.getID())).findFirst();
        if (node.isEmpty()) this.proposals.put(participant, Optional.empty());
        this.confirmed.add(participant);
    }

    public void onAuctionRejected(Auction auction, INode participant) {
        this.log.debug("Agent {} rejected auction", participant.getID());
        this.participants.removeIf(n -> n.getID().equals(participant.getID()));

        if (this.isSaturated()) {
            this.finalizeAuction(auction);
        } else {
            var _participants = this.participants.stream()
                    .map(n -> n.getID())
                    .collect(Collectors.joining(", "));
            var _proposals = this.proposals.keySet().stream()
                    .map(n -> n.getID())
                    .collect(Collectors.joining(", "));
            var _confirmed = this.confirmed.stream()
                    .map(n -> n.getID())
                    .collect(Collectors.joining(", "));
            this.log.debug("\n{}\n{}\n{}\n", _participants, _proposals, _confirmed);
            this.log.debug("Received {} proposals, waiting for {} more (confirmed: {})", this.proposals.size(),
                    this.participants.size() - this.proposals.size(), this.confirmed.size());
        }
    }

    private void finalizeAuction(Auction auction) {
        this.log.info("Finalizing auction {}, trying to reduce damage value {}", auction.getId(),
                auction.getRiskReport().damage());
        try {
            var path = Path.of("./graphs/" + auction.getId() + "/auction.txt");
            Files.createDirectories(path.getParent());
            var writer = new FileWriter(path.toFile());
            writer.write("Host: " + auction.getHost().getID() + "\n");
            writer.write("Risk: " + auction.getRiskReport().toString() + "\n");
            writer.write("Joined: " + confirmed.stream().map(INode::getID).collect(Collectors.joining(", ")) + "\n");
            writer.write("Proposals:\n");

            for (var node : proposals.keySet()) {
                var p = proposals.get(node);
                writer.write(p.isPresent() && p.get().mutation() != null
                        ? String.format("- proposal from %s: reducing to %.2f by %s", p.get().origin().getID(),
                        p.get().updatedReport().damage(), p.get().mutation())
                        : String.format("- proposal from %s: nothing",
                        p.isPresent() ? p.get().origin().getID() : "unknown") + "\n");
            }
            writer.close();
        } catch (Exception e) {
            log.error(e);
        }

        this.timeout.cancel();

        if (proposals.isEmpty()) {
            this.log.warn("Auction did not yield any applicable proposals, ignoring auction results");
            this.log.info("-- Auction Stopped!! {}", auction.getId());
            this.reset();
        }

        var proposal = this.proposalSelector.select(proposals.values().stream()
                        .filter(p -> p.isPresent())
                        .map(p -> p.get())
                        .toList(),
                auction.getRiskReport().damage() - 0.1f);
        if (proposal.isEmpty()) {
            this.participants.forEach(
                    node -> this.messageBroker.send(node, new EventMessage<>(new AuctionCancelledEvent(auction))));
            this.log.info("-- Auction Stopped!! {}", auction.getId());
            this.reset();
            return;
        }


        this.log.info("Selected proposal from {}: reducing to {} by {}",
                proposal.get().origin().getID(),
                proposal.get().updatedReport().damage(),
                proposal.get().mutation().toString());

        // TODO: If no proposal was sent, we might not want to sent this event
        var event = new AuctionFinalizedEvent(auction, proposal.get());
        this.participants.forEach(node -> {
            if (node.equals(this.configuration.getNodeID())) {
                this.eventManager.emit(event);
            } else {
                var message = new EventMessage<>(event);
                this.messageBroker.send(node, message);
            }
        });

        this.log.info("-- Auction Stopped!! {}", auction.getId());
        this.reset();
    }

    public boolean isAuctioning() {
        return this.auction.current() != null;
    }

    public SubscribableValueEvent<Auction> onAuctionChanged() {
        return this.auction.subscribable;
    }

    private boolean isSaturated() {
        var validProposalCount = this.proposals.values().stream().filter(Optional::isPresent).count();
        this.log.debug("participants {}, confirmed {}, proposals {}, saturated {}",
                participants.size(),
                confirmed.size(),
                validProposalCount,
                validProposalCount == participants.size()
                        && proposals.values().stream().allMatch(Optional::isPresent));
        return validProposalCount == participants.size()
                && proposals.values().stream().allMatch(Optional::isPresent);
    }

    public void reset() {
        this.auction.setCurrent(null);
        this.proposals = new ConcurrentHashMap<>();
        this.participants = ConcurrentHashMap.newKeySet();
        this.confirmed = ConcurrentHashMap.newKeySet();
        this.log.debug("reset auctioning state");
    }
}
