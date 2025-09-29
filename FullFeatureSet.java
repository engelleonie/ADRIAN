package tech.jorn.adrian.experiment.features;

import tech.jorn.adrian.agent.AgentConfiguration;
import tech.jorn.adrian.agent.controllers.*;
import tech.jorn.adrian.agent.events.SendMessageEvent;
import tech.jorn.adrian.agent.services.AuctionManager;
import tech.jorn.adrian.core.agents.AgentState;
import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.controllers.IController;
import tech.jorn.adrian.core.events.queue.InMemoryQueue;
import tech.jorn.adrian.core.graphs.base.INode;
import tech.jorn.adrian.core.graphs.infrastructure.Infrastructure;
import tech.jorn.adrian.core.graphs.infrastructure.InfrastructureNode;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBase;
import tech.jorn.adrian.core.messages.EventMessage;
import tech.jorn.adrian.core.observables.EventDispatcher;
import tech.jorn.adrian.core.observables.ValueDispatcher;
import tech.jorn.adrian.core.services.probability.ProductRiskProbability;
import tech.jorn.adrian.core.services.proposals.LowestDamage;
import tech.jorn.adrian.core.services.proposals.ProposalManager;
import tech.jorn.adrian.core.services.risks.HighestRisk;
import tech.jorn.adrian.experiment.ExperimentalInfrastructureEffector;
import tech.jorn.adrian.experiment.instruments.ExperimentalAgent;
import tech.jorn.adrian.experiment.instruments.ExperimentalEventManager;
import tech.jorn.adrian.experiment.instruments.ExperimentalRiskDetection;
import tech.jorn.adrian.experiment.messages.Envelope;
import tech.jorn.adrian.experiment.messages.InMemoryBroker;
import tech.jorn.adrian.experiment.messages.ThreadedBroker;
import tech.jorn.adrian.risks.RiskLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FullFeatureSet extends FeatureSet {

    private final EventDispatcher<Envelope> messageDispatcher;

    public FullFeatureSet(EventDispatcher<Envelope> messageDispatcher) {
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    IAgent getAgent(Infrastructure infrastructure, InfrastructureNode node) {
        var neighbours = this.getNeighboursFromInfrastructure(infrastructure, node);

        var assets = this.getAssetsFromInfrastructure(infrastructure, node);
        var configuration = new AgentConfiguration(node, neighbours, assets);

        var messageQueue = new InMemoryQueue();
        var knowledgeBase = new KnowledgeBase();

        var probabilityCalculator = new ProductRiskProbability();
        var agentState = new ValueDispatcher<>(AgentState.Initializing);
        var infrastructureEffector = new ExperimentalInfrastructureEffector(infrastructure);

        var messageBroker = new InMemoryBroker(node, neighbours, this.messageDispatcher);

        // Services
        var eventManager = new ExperimentalEventManager(messageQueue, configuration, agentState.subscribable, messageBroker);
        eventManager.registerEventHandler(SendMessageEvent.class, event -> {
            var sendEvent = (SendMessageEvent) event;
            messageBroker.send(sendEvent.getRecipient(), sendEvent.getMessage());
        });
        var riskDetection = new ExperimentalRiskDetection(RiskLoader.listRisks(), probabilityCalculator, configuration);
        var proposalManager = new ProposalManager(knowledgeBase, riskDetection, new LowestDamage(100.0f), configuration, agentState, infrastructureEffector);


        var agent = new ExperimentalAgent(messageBroker, eventManager, riskDetection, knowledgeBase, new ArrayList<>(), configuration, agentState, node.getID());



        var auctionManager = new AuctionManager(messageBroker, eventManager, new LowestDamage(100.0f), configuration);


        List<IController> controllers = List.of(
                new KnowledgeController(knowledgeBase, messageBroker, eventManager, configuration, agentState.subscribable, node.getID()),
                new RiskController(riskDetection, knowledgeBase, eventManager, new HighestRisk(1.0f), agent),
                new ProposalController(proposalManager, eventManager, agentState.subscribable),
                new AuctionController(auctionManager, eventManager, configuration, agentState),
                new SystemController(eventManager, agentState.subscribable),
                new SleepController(agent)
        );

        agent.getControllers().addAll(controllers);


        messageBroker.registerMessageHandler(message -> {
            if (message instanceof EventMessage<?> m) eventManager.emit(m.getEvent());
        });

        return agent;
    }
}
