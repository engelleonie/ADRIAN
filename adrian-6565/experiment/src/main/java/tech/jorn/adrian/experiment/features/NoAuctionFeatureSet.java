package tech.jorn.adrian.experiment.features;

import tech.jorn.adrian.agent.AgentConfiguration;
import tech.jorn.adrian.agent.controllers.KnowledgeController;
import tech.jorn.adrian.agent.controllers.ProposalController;
import tech.jorn.adrian.agent.controllers.RiskController;
import tech.jorn.adrian.agent.events.ShareKnowledgeEvent;
import tech.jorn.adrian.core.agents.AgentState;
import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.controllers.IController;
import tech.jorn.adrian.core.events.queue.InMemoryQueue;
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
import tech.jorn.adrian.experiment.ExperimentalConfiguration;
import tech.jorn.adrian.experiment.ExperimentalInfrastructureEffector;
import tech.jorn.adrian.experiment.instruments.ProposalImplementationController;
import tech.jorn.adrian.experiment.instruments.ExperimentalAgent;
import tech.jorn.adrian.experiment.instruments.ExperimentalEventManager;
import tech.jorn.adrian.experiment.instruments.ExperimentalRiskDetection;
import tech.jorn.adrian.experiment.messages.Envelope;
import tech.jorn.adrian.experiment.messages.InMemoryBroker;
import tech.jorn.adrian.risks.RiskLoader;

import java.util.List;

public class NoAuctionFeatureSet extends FeatureSet {

    private final EventDispatcher<Envelope> messageDispatcher;

    public NoAuctionFeatureSet(EventDispatcher<Envelope> messageDispatcher) {
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    IAgent getAgent(Infrastructure infrastructure, InfrastructureNode node) {
        var neighbours = this.getNeighboursFromInfrastructure(infrastructure, node);
        var assets = this.getAssetsFromInfrastructure(infrastructure, node);
        var configuration = new ExperimentalConfiguration(node, neighbours, assets);

        var messageQueue = new InMemoryQueue();
        var knowledgeBase = new KnowledgeBase();

        var probabilityCalculator = new ProductRiskProbability();
        var agentState = new ValueDispatcher<>(AgentState.Initializing);
        var infrastructureEffector = new ExperimentalInfrastructureEffector(infrastructure);

        // Services
        var eventManager = new ExperimentalEventManager(messageQueue, configuration, agentState.subscribable);
        var riskDetection = new ExperimentalRiskDetection(RiskLoader.listRisks(), probabilityCalculator, configuration);
        var proposalManager = new ProposalManager(knowledgeBase, riskDetection, new LowestDamage(100.0f), configuration, agentState, infrastructureEffector);

        var messageBroker = new InMemoryBroker(node, neighbours, this.messageDispatcher);


        List<IController> controllers = List.of(
                new KnowledgeController(knowledgeBase, messageBroker, eventManager, configuration, agentState.subscribable),
                new RiskController(riskDetection, knowledgeBase, eventManager, new HighestRisk(1.0f), configuration, agentState.subscribable),
                new ProposalController(proposalManager, eventManager, agentState.subscribable),
                new ProposalImplementationController(eventManager, configuration, agentState.subscribable)
        );

        var agent = new ExperimentalAgent(messageBroker, eventManager, riskDetection, knowledgeBase, controllers, configuration, agentState);

        messageBroker.registerMessageHandler(message -> {
            if (message instanceof EventMessage<?> m) eventManager.emit(m.getEvent());
        });

        return agent;
    }
}
