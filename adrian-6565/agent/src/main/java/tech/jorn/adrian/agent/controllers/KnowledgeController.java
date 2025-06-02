package tech.jorn.adrian.agent.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.jorn.adrian.agent.events.IdentifyRiskEvent;
import tech.jorn.adrian.agent.events.ShareKnowledgeEvent;
import tech.jorn.adrian.core.agents.AgentState;
import tech.jorn.adrian.core.agents.IAgentConfiguration;
import tech.jorn.adrian.core.controllers.AbstractController;
import tech.jorn.adrian.core.events.EventManager;
import tech.jorn.adrian.core.graphs.AbstractDetailedNode;
import tech.jorn.adrian.core.graphs.MermaidGraphRenderer;
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
import tech.jorn.adrian.core.properties.SoftwareProperty;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

public class KnowledgeController extends AbstractController {
    Logger log = LogManager.getLogger(KnowledgeController.class);

    private final KnowledgeBase knowledgeBase;
    private final MessageBroker messageBroker;
    private final IAgentConfiguration configuration;
    private boolean hasSharedInitialKnowledge = false;
    private boolean triggerRiskIdentificationOnIdle = false;

    private ScheduledExecutorService knowledgeShareScheduler = Executors.newSingleThreadScheduledExecutor();
    private Future<?> knowledgeShareFuture;

    public KnowledgeController(KnowledgeBase knowledgeBase, MessageBroker messageBroker, EventManager eventManager,
            IAgentConfiguration configuration, SubscribableValueEvent<AgentState> agentState) {
        super(eventManager, agentState);
        this.messageBroker = messageBroker;
        this.configuration = configuration;

        log = LogManager.getLogger("[" + configuration.getNodeID() + "] KnowledgeController");

        this.knowledgeBase = this.createKnowledgeBaseFromConfig(knowledgeBase, configuration.getParentNode(),
                configuration.getNeighbours(), configuration.getAssets());

        this.eventManager.registerEventHandler(ShareKnowledgeEvent.class, this::processKnowledge);
        this.configuration.getParentNode().onPropertyChange().subscribe(this::debouncedPropertyChange);
        this.configuration.getAssets()
                .forEach(asset -> asset.onPropertyChange().subscribe(() -> this.onAssetPropertyChange(asset)));

        agentState.subscribe(state -> {
            if (state == AgentState.Idle && (!this.hasSharedInitialKnowledge || triggerRiskIdentificationOnIdle)) {
                this.shareKnowledge();
                this.hasSharedInitialKnowledge = true;
                this.triggerRiskIdentificationOnIdle = false;
            }
        });
    }

    protected void processKnowledge(ShareKnowledgeEvent event) {
        if (knowledgeBase.findById(event.getOrigin().getID()).isEmpty() && event.getDistance() == 1) {
            this.messageBroker.addRecipient(event.getOrigin());
            this.log.info("Added a new neighbour {}", event.getOrigin().getID());
        }

        this.knowledgeBase.processNewInformation(event.getOrigin(), event.getKnowledgeBase());

        if (this.agentState.current().equals(AgentState.Idle))
            this.eventManager.emit(new IdentifyRiskEvent());

        if (event.getDistance() > 1) {
            var next = ShareKnowledgeEvent.reducedDistance(event);
            this.messageBroker.broadcast(new EventMessage<>(next));
        }
    }

    protected void debouncedPropertyChange(NodeProperty<?> property) {
        if (this.knowledgeShareFuture != null && !this.knowledgeShareFuture.isDone()) {
            this.knowledgeShareFuture.cancel(true);
        }
        this.knowledgeShareFuture = this.knowledgeShareScheduler.schedule(() -> {
            this.onNodePropertyChange(property);
        }, 100, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    protected void onNodePropertyChange(NodeProperty<?> property) {
        var node = this.configuration.getParentNode();
        this.log.debug("Updating property {} from {} to {}", property.getName(),
                this.knowledgeBase.findById(node.getID()).get().getProperty(property.getName()), property.getValue());
        this.knowledgeBase.upsertNode(KnowledgeBaseNode.fromNode(node)
                .setKnowledgeOrigin(KnowledgeOrigin.DIRECT));

        this.shareKnowledge();

        // this.triggerRiskIdentificationOnIdle = true;
        if (this.agentState.current().equals(AgentState.Idle))
            this.eventManager.emit(new IdentifyRiskEvent());
    }

    protected void onAssetPropertyChange(SoftwareAsset asset) {
        this.knowledgeBase.upsertNode(KnowledgeBaseSoftwareAsset.fromNode(asset)
                .setKnowledgeOrigin(KnowledgeOrigin.DIRECT));

        this.shareKnowledge();

        // this.triggerRiskIdentificationOnIdle = true;
        if (this.agentState.current().equals(AgentState.Idle))
            this.eventManager.emit(new IdentifyRiskEvent());
    }

    protected void shareKnowledge() {
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
        return knowledgeBase;
    }
}
