package tech.jorn.adrian.agent.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tech.jorn.adrian.agent.events.IdentifyRiskEvent;
import tech.jorn.adrian.agent.events.SendMessageEvent;
import tech.jorn.adrian.agent.events.ShareKnowledgeEvent;
import tech.jorn.adrian.core.agents.AgentState;
import tech.jorn.adrian.core.agents.IAgentConfiguration;
import tech.jorn.adrian.core.controllers.AbstractController;
import tech.jorn.adrian.core.events.EventManager;
import tech.jorn.adrian.core.graphs.AbstractDetailedNode;
import tech.jorn.adrian.core.graphs.base.INode;
import tech.jorn.adrian.core.graphs.base.VoidNode;
import tech.jorn.adrian.core.graphs.infrastructure.SoftwareAsset;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBase;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBaseNode;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBaseSoftwareAsset;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeOrigin;
import tech.jorn.adrian.core.messages.EventMessage;
import tech.jorn.adrian.core.messages.MessageBroker;
import tech.jorn.adrian.core.observables.SubscribableValueEvent;
import tech.jorn.adrian.core.properties.NodeProperty;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class KnowledgeController extends AbstractController {
    Logger log = LogManager.getLogger(KnowledgeController.class);

    private final KnowledgeBase knowledgeBase;
    private final MessageBroker messageBroker;
    private final IAgentConfiguration configuration;
    private final String nodeID;
    private int onAssetPropertyChange = 0;
    private  int onNodePropertyChange = 0;
    private int reducedCount = 0;
    private final Set<String> seenKnowledgeOrigins = new HashSet<>();

    public KnowledgeController(KnowledgeBase knowledgeBase, MessageBroker messageBroker, EventManager eventManager,
            IAgentConfiguration configuration, SubscribableValueEvent<AgentState> agentState, String nodeID) {
        super(eventManager, agentState);
        this.messageBroker = messageBroker;
        this.configuration = configuration;
        this.nodeID = nodeID;

        log = LogManager.getLogger("[" + configuration.getNodeID() + "] KnowledgeController");

        this.knowledgeBase = this.createKnowledgeBaseFromConfig(knowledgeBase, configuration.getParentNode(),
                configuration.getNeighbours(), configuration.getAssets());

        this.eventManager.registerEventHandler(ShareKnowledgeEvent.class, this::processKnowledge);
        this.eventManager.registerEventHandler(SendMessageEvent.class, this::processMessage);


        this.configuration.getParentNode().onPropertyChange().subscribe(this::debouncedPropertyChange);
        this.configuration.getAssets()
                .forEach(asset -> asset.onPropertyChange().subscribe(() -> this.onAssetPropertyChange(asset)));

        /* agentState.subscribe(state -> {
            if (state == AgentState.Idle && (!this.hasSharedInitialKnowledge || triggerRiskIdentificationOnIdle)) {
                this.shareKnowledge();
                this.hasSharedInitialKnowledge = true;
                this.triggerRiskIdentificationOnIdle = false;
            }
        }); */
    }

    protected void processKnowledge(ShareKnowledgeEvent event) {
        if (event == null || event.getOrigin() == null || event.getKnowledgeBase() == null) return;

        String originId = event.getOrigin().getID();

        if (seenKnowledgeOrigins.contains(originId)) {
            log.debug("Knowledge from origin {} already processed, skipping", originId);
            return;
        }

        seenKnowledgeOrigins.add(originId);

        boolean newInformationAdded = false;

        try {
            this.knowledgeBase.processNewInformation(
                    event.getOrigin(),
                    event.getKnowledgeBase()
            );
        } catch (Exception e) {
            log.error("Exception in processNewInformation: {}", e.getMessage(), e);
            return;
        }

        if (newInformationAdded) {
            log.info("Agent {} acquired new knowledge from {}, triggering risk identification",
                    this.nodeID, originId);

            this.eventManager.emit(new IdentifyRiskEvent(this.nodeID));
        } else {
            log.debug("Agent {} received knowledge from {}, but nothing new was added",
                    this.nodeID, originId);
        }

        if (event.getDistance() > 1) {
            ShareKnowledgeEvent next = event.reducedDistance(event);
            this.messageBroker.broadcast(new EventMessage<>(next));
            log.debug("Agent {} forwarded knowledge from {} (new distance={})",
                    this.nodeID, originId, next.getDistance());
        } else {
            log.trace("Knowledge from {} not forwarded (distance limit reached)", originId);
        }
    }


    private void processMessage(SendMessageEvent event) {
        this.messageBroker.deliver(event.getRecipient(), event.getMessage());
    }

    protected void debouncedPropertyChange(NodeProperty<?> property) {
            this.onNodePropertyChange(property);
    }

    protected void onNodePropertyChange(NodeProperty<?> property) {
        var node = this.configuration.getParentNode();
        this.log.debug("Updating property {} from {} to {}", property.getName(),
                this.knowledgeBase.findById(node.getID()).get().getProperty(property.getName()), property.getValue());
        this.knowledgeBase.upsertNode(KnowledgeBaseNode.fromNode(node)
                .setKnowledgeOrigin(KnowledgeOrigin.DIRECT));

        this.shareKnowledge();

        if (this.agentState.current().equals(AgentState.Idle))
            this.eventManager.emit(new IdentifyRiskEvent(nodeID));
        onNodePropertyChange++;
        System.out.println("On Node property change: " + onNodePropertyChange);
    }

    protected void onAssetPropertyChange(SoftwareAsset asset) {
        this.knowledgeBase.upsertNode(KnowledgeBaseSoftwareAsset.fromNode(asset)
                .setKnowledgeOrigin(KnowledgeOrigin.DIRECT));

        this.shareKnowledge();

        if (this.agentState.current().equals(AgentState.Idle))
            this.eventManager.emit(new IdentifyRiskEvent(nodeID));
        //
        onAssetPropertyChange++;
        System.out.println("On asset property change: " + onAssetPropertyChange);
    }

    public void shareKnowledge() {
        var event = new ShareKnowledgeEvent(
                this.configuration.getParentNode(),
                this.knowledgeBase,
                1);
        this.messageBroker.broadcast(new EventMessage<>(event));
    }

    private KnowledgeBase createKnowledgeBaseFromConfig(KnowledgeBase knowledgeBase,
            AbstractDetailedNode<NodeProperty<?>> origin, List<String> links, List<SoftwareAsset> assets) {

        var voidNode = VoidNode.forKnowledge();
        knowledgeBase.upsertNode(voidNode);

        var originNode = KnowledgeBaseNode.fromNode(origin)
                .setKnowledgeOrigin(KnowledgeOrigin.DIRECT);
        knowledgeBase.upsertNode(originNode);

        assets.forEach(asset -> {
            var assetNode = KnowledgeBaseSoftwareAsset.fromNode(asset);
            knowledgeBase.upsertNode(assetNode);
            knowledgeBase.addEdge(originNode, assetNode);
            knowledgeBase.addEdge(assetNode, originNode);
        });

        links.forEach(neighborId -> {
            var neighborNode = new KnowledgeBaseNode(neighborId)
                    .setKnowledgeOrigin(KnowledgeOrigin.INFERRED);
            knowledgeBase.upsertNode(neighborNode);
            knowledgeBase.addEdge(originNode, neighborNode);
            knowledgeBase.addEdge(neighborNode, originNode);
        });

        var isExposed = (Boolean) origin.getProperty("exposed").orElse(false);
        if (isExposed)
            knowledgeBase.addEdge(voidNode, originNode);

        assets.forEach(asset -> {
            var assetNode = KnowledgeBaseSoftwareAsset.fromNode(asset);
            knowledgeBase.upsertNode(assetNode);
            knowledgeBase.addEdge(originNode, assetNode);
            knowledgeBase.addEdge(assetNode, originNode);
        });
        links.forEach(node -> {
            var neighbourNode = new KnowledgeBaseNode(node)
                    .setKnowledgeOrigin(KnowledgeOrigin.INFERRED);
            knowledgeBase.upsertNode(neighbourNode);
            knowledgeBase.addEdge(originNode, neighbourNode);
            knowledgeBase.addEdge(neighbourNode, originNode);
        });

        /* System.out.println("KnowledgeBase dump:");
        knowledgeBase.getNodes().forEach(n -> {
            var neighbours = knowledgeBase.getNeighbours(n);
            System.out.println("  " + n.getID() + " -> " +
                    neighbours.stream().map(x -> x.getID()).toList());
        }); */

        return knowledgeBase;
    }
}
