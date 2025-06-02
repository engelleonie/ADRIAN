package tech.jorn.adrian.agent;

import tech.jorn.adrian.agent.controllers.AuctionController;
import tech.jorn.adrian.agent.controllers.KnowledgeController;
import tech.jorn.adrian.agent.controllers.ProposalController;
import tech.jorn.adrian.agent.controllers.RiskController;
import tech.jorn.adrian.agent.services.AuctionManager;
import tech.jorn.adrian.agent.services.BasicRiskDetection;
import tech.jorn.adrian.core.agents.AgentState;
import tech.jorn.adrian.core.agents.IAgentConfiguration;
import tech.jorn.adrian.core.controllers.IController;
import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.events.EventManager;
import tech.jorn.adrian.core.events.queue.InMemoryQueue;
import tech.jorn.adrian.core.graphs.base.INode;
import tech.jorn.adrian.core.graphs.infrastructure.Infrastructure;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBase;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBaseNode;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeOrigin;
import tech.jorn.adrian.core.messages.EventMessage;
import tech.jorn.adrian.core.observables.EventDispatcher;
import tech.jorn.adrian.core.observables.ValueDispatcher;
import tech.jorn.adrian.core.messages.Message;
import tech.jorn.adrian.core.messages.MessageBroker;
import tech.jorn.adrian.core.services.InfrastructureEffector;
import tech.jorn.adrian.core.services.proposals.ProposalManager;
import tech.jorn.adrian.core.services.probability.ProductRiskProbability;
import tech.jorn.adrian.core.services.proposals.LowestDamage;
import tech.jorn.adrian.core.services.risks.HighestRisk;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class AgentRunner {
    public static void main(String[] args) {
        var configuration = (IAgentConfiguration) null;
        var queue = new InMemoryQueue();
        var agentState = new ValueDispatcher<>(AgentState.Initializing);
        var eventManager = new EventManager(queue, agentState.subscribable);
        var messageBroker = new InMemoryBroker();

        var infrastructureEffector = new InfrastructureEffector() {
            @Override
            public void updateInfra(Consumer<Infrastructure> fn) {
                // Void
            }
        };

        var knowledgeBase = new KnowledgeBase();
        var riskDetection = new BasicRiskDetection(List.of(), new ProductRiskProbability(), configuration);
        var proposalManager = new ProposalManager(knowledgeBase, riskDetection, new LowestDamage(2.0f), configuration, agentState, infrastructureEffector);

        KnowledgeBaseNode parent = KnowledgeBaseNode.fromNode(configuration.getParentNode(), KnowledgeOrigin.DIRECT);
        knowledgeBase.upsertNode(parent);

        List<IController> controllers = Arrays.asList(
                new RiskController(riskDetection, knowledgeBase, eventManager, new HighestRisk(1.0f), configuration, agentState.subscribable),
                new AuctionController(new AuctionManager(messageBroker, eventManager, new LowestDamage(2.0f), configuration), eventManager, configuration, agentState),
                new KnowledgeController(knowledgeBase, messageBroker, eventManager, configuration, agentState.subscribable),
                new ProposalController(proposalManager, eventManager, agentState.subscribable)
        );
        var agent = new AdrianAgent(controllers, configuration, agentState);

        messageBroker.registerMessageHandler(message -> {
            if (message instanceof EventMessage<?> m) eventManager.emit(m.getEvent());
        });

        var thread = new Thread(agent::start);
        thread.start();
    }
}

class InMemoryBroker implements MessageBroker {
    private final EventDispatcher<Event> messageDispatcher = new EventDispatcher<>();

    public InMemoryBroker() {

    }

    @Override
    public void send(INode recipient, Message message) {

    }

    @Override
    public void broadcast(Message message) {

    }

    @Override
    public void addRecipient(INode recipient) {

    }

    @Override
    public void registerMessageHandler(Consumer<Message> messageHandler) {

    }
}
