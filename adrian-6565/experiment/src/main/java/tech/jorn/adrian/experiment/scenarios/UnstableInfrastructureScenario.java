package tech.jorn.adrian.experiment.scenarios;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.graphs.infrastructure.Infrastructure;
import tech.jorn.adrian.core.graphs.infrastructure.InfrastructureNode;
import tech.jorn.adrian.core.observables.EventDispatcher;
import tech.jorn.adrian.experiment.instruments.ExperimentalAgent;
import tech.jorn.adrian.experiment.messages.Envelope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;

public class UnstableInfrastructureScenario extends Scenario {
    Logger log = LogManager.getLogger(GrowingInfrastructureScenario.class);
    private final Function<InfrastructureNode, IAgent> agentFactory;

    public UnstableInfrastructureScenario(Infrastructure infrastructure, EventDispatcher<Envelope> messageDispatcher, Function<InfrastructureNode, IAgent> agentFactory) {
        super(infrastructure, messageDispatcher, 10 * 60 * 1000);
        this.agentFactory = agentFactory;
    }

    @Override
    public void onScheduleEvents(Queue<ExperimentalAgent> agents) {

        after(30 * 1000, () -> {
            var nodeId = "soc-temperature";
            var agent = agents.stream().filter(a -> a.getConfiguration().getNodeID().equals(nodeId)).findFirst().get();
            var node = infrastructure.findById(nodeId);
            this.log.info("Removing node {} from infrastructure", node.get().getID());
            var links = infrastructure.getLinks(node.get());
            agents.remove(agent);
            infrastructure.removeNode(nodeId);

            agent.stop();

            after(60 * 1000, () -> {
                this.log.info("Adding node {} back into the infrastructure", node.get().getID());
                infrastructure.upsertNode(node.get());
                links.forEach(link -> infrastructure.addEdge(node.get(), link.getNode()));
                links.forEach(link -> infrastructure.addEdge(link.getNode(), node.get()));

                var newAgent = (ExperimentalAgent) this.agentFactory.apply((InfrastructureNode) node.get());
                newAgent.start();
                agents.add(newAgent);
            });
        });

        after(90 * 1000, () -> {
            var nodeId = "soc-gate";
            var agent = agents.stream().filter(a -> a.getConfiguration().getNodeID().equals(nodeId)).findFirst().get();
            var node = infrastructure.findById(nodeId);
            this.log.info("Removing node {} from infrastructure", node.get().getID());
            var links = infrastructure.getLinks(node.get());
            agents.remove(agent);
            infrastructure.removeNode(nodeId);

            agent.stop();

            after(60 * 1000, () -> {
                this.log.info("Adding node {} back into the infrastructure", node.get().getID());
                infrastructure.upsertNode(node.get());
                links.forEach(link -> infrastructure.addEdge(node.get(), link.getNode()));
                links.forEach(link -> infrastructure.addEdge(link.getNode(), node.get()));

                var newAgent = (ExperimentalAgent) this.agentFactory.apply((InfrastructureNode) node.get());
                newAgent.start();
                agents.add(newAgent);
            });
        });

        this.after(4 * 60 * 1000, () -> {
            this.log.debug("Waiting for silence");
            this.waitForSilence(1000, this.finished::raise);
        });
    }
}
