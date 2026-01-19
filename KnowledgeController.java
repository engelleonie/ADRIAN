package tech.jorn.adrian.agent.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tech.jorn.adrian.agent.NodeRegistry;
import tech.jorn.adrian.agent.events.IdentifyRiskEvent;
import tech.jorn.adrian.agent.events.SendMessageEvent;
import tech.jorn.adrian.agent.events.ShareKnowledgeEvent;
import tech.jorn.adrian.core.GlobalQueue;
import tech.jorn.adrian.core.agents.AgentState;
import tech.jorn.adrian.core.agents.IAgent;
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
import tech.jorn.adrian.core.messages.Message;
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

    private int onAssetPropertyChange = 0;
    private  int onNodePropertyChange = 0;

    //created for each agent along with 3 other controllers
    public KnowledgeController(KnowledgeBase knowledgeBase, MessageBroker messageBroker, EventManager eventManager,
            IAgentConfiguration configuration, SubscribableValueEvent<AgentState> agentState, String nodeID) {
        super(eventManager, agentState);
        this.messageBroker = messageBroker;
        this.configuration = configuration;

        log = LogManager.getLogger("[" + configuration.getNodeID() + "] KnowledgeController");

        //initializing an agents knowledgeBase
        this.knowledgeBase = this.createKnowledgeBaseFromConfig(knowledgeBase, configuration.getParentNode(),
                configuration.getNeighbours(), configuration.getAssets());

        //calling processKnowledge() when executing ShareKnowledgeEvent
        //this.eventManager.registerEventHandler(ShareKnowledgeEvent.class, this::processKnowledge);
        this.eventManager.registerEventHandler(SendMessageEvent.class, this::processMessage);


        //reacting to changed properties
        this.configuration.getParentNode().onPropertyChange().subscribe(this::debouncedPropertyChange);
        //reacting to changed assets
        this.configuration.getAssets()
                .forEach(asset -> asset.onPropertyChange().subscribe(() -> this.onAssetPropertyChange(asset)));
    }

    protected void processKnowledge(ShareKnowledgeEvent event) {
        //this.messageBroker.broadcast(new EventMessage<>(event));

        if (knowledgeBase.findById(event.getOrigin().getID()).isEmpty() && event.getDistance() == 1) {
            this.messageBroker.addRecipient(event.getOrigin());
            this.log.info("Added a new neighbour {}", event.getOrigin().getID());
        }
        this.knowledgeBase.processNewInformation(event.getOrigin(), event.getKnowledgeBase());

        if (event.getDistance() > 1) {

            ShareKnowledgeEvent next =
                    ShareKnowledgeEvent.reducedDistance(event);

            for (String neighbourId : configuration.getNeighbours()) {

                if (neighbourId.equals(event.getOrigin().getID())) {
                    continue;
                }

                IAgent recipient =
                        NodeRegistry.getInstance().getAgentByNodeId(neighbourId);
                if (recipient == null) {
                    continue;
                }

                GlobalQueue.getInstance().offer(
                        new SendMessageEvent(
                                configuration.getParentNode(),          // sender
                                NodeRegistry.getInstance().getNodeById(neighbourId),
                                new EventMessage<>(next),               // Payload!
                                recipient
                        ),
                        5
                );
            }
        }
        //this.messageBroker.broadcast(new EventMessage<>(event));

            /*if (event == null || event.getOrigin() == null || event.getKnowledgeBase() == null) {
                System.out.println(667);
                return;
            } */
/*
            String originId = event.getOrigin().getID();
            boolean isDirectNeighbor = event.getDistance() == 1;

            //testing if knowledge contains a new neighbor
            boolean isNewNeighbor = isDirectNeighbor &&
                    !originId.equals(this.configuration.getParentNode().getID()) &&
                    knowledgeBase.findById(originId).isEmpty();

            //adding new neighbor
            if (isNewNeighbor) {
                this.messageBroker.addRecipient(event.getOrigin());
                this.log.info("Added a new neighbour {}", originId);
            }

            //updating knowledgeBase
        System.out.println("EventID: " + event.getOrigin().getID());
            this.knowledgeBase.processNewInformation(event.getOrigin(), event.getKnowledgeBase());



            // distributing knowledge further in case it is supposed to travel more than one hop
            if (event.getDistance() > 1) {
                var next = ShareKnowledgeEvent.reducedDistance(event);
                this.messageBroker.broadcast(new EventMessage<>(next));
            }
*/
        }


    private void processMessage(SendMessageEvent event) {

        if (!event.getRecipient().getID().equals(configuration.getNodeID())) {
            return;
        }
        //correct spot?
        //this.messageBroker.deliver(event.getRecipient(), event.getMessage());

        Message msg = event.getMessage();

        if (msg instanceof EventMessage<?> em &&
                em.getEvent() instanceof ShareKnowledgeEvent ske) {

            processKnowledge(ske);
        }
    }




    protected void debouncedPropertyChange(NodeProperty<?> property) {
            this.onNodePropertyChange(property);
    }

    protected void onNodePropertyChange(NodeProperty<?> property) {
        var node = this.configuration.getParentNode();
        this.log.debug("Updating property {} from {} to {}", property.getName(),
                this.knowledgeBase.findById(node.getID()).get().getProperty(property.getName()), property.getValue());
        //adding changed property to knowledgeBase
        this.knowledgeBase.upsertNode(KnowledgeBaseNode.fromNode(node)
                .setKnowledgeOrigin(KnowledgeOrigin.DIRECT));

        //share updated property
        this.shareKnowledge();

        if (this.agentState.current().equals(AgentState.Idle))
            this.eventManager.emit(new IdentifyRiskEvent(NodeRegistry.getInstance().getAgentByNodeId(configuration.getNodeID())));

        //debugging
        onNodePropertyChange++;
        System.out.println("On Node property change: " + onNodePropertyChange);
    }

    protected void onAssetPropertyChange(SoftwareAsset asset) {
        //adding updated asset to knowledgeBase
        this.knowledgeBase.upsertNode(KnowledgeBaseSoftwareAsset.fromNode(asset)
                .setKnowledgeOrigin(KnowledgeOrigin.DIRECT));

        //share updated asset
        this.shareKnowledge();

        if (this.agentState.current().equals(AgentState.Idle))
            this.eventManager.emit(new IdentifyRiskEvent(NodeRegistry.getInstance().getAgentByNodeId(configuration.getNodeID())));

        //debugging
        onAssetPropertyChange++;
        System.out.println("On asset property change: " + onAssetPropertyChange);
    }

    public void shareKnowledge() {



        for (String neighbourId : configuration.getNeighbours()) {

            var event = new ShareKnowledgeEvent(
                    this.configuration.getParentNode(),
                    this.knowledgeBase,
                    1
            );

            IAgent recipientAgent =
                    NodeRegistry.getInstance().getAgentByNodeId(neighbourId);
            INode recipientNode =
                    NodeRegistry.getInstance().getNodeById(neighbourId);
            if (recipientAgent == null) continue;

            GlobalQueue.getInstance().offer(
                    new SendMessageEvent(
                            configuration.getParentNode(),
                            recipientNode,
                            new EventMessage<>(event),
                            recipientAgent
                    ),
                    5
            );
        }
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

        //debugging to check which nodes are included in an agents' knowledgeBase
        System.out.println("KnowledgeBase dump:");
        knowledgeBase.getNodes().forEach(n -> {
            var neighbours = knowledgeBase.getNeighbours(n);
            System.out.println("  " + n.getID() + " -> " +
                    neighbours.stream().map(x -> x.getID()).toList());
        });

        return knowledgeBase;
    }
}
