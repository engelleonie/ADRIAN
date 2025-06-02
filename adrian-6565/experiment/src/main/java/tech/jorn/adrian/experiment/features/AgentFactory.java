package tech.jorn.adrian.experiment.features;

import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.graphs.infrastructure.Infrastructure;
import tech.jorn.adrian.core.graphs.infrastructure.InfrastructureNode;
import tech.jorn.adrian.experiment.instruments.ExperimentalAgent;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AgentFactory {
    private final FeatureSet featureSet;

    public AgentFactory(FeatureSet featureSet) {
        this.featureSet = featureSet;
    }


    public Queue<ExperimentalAgent> fromInfrastructure(Infrastructure infrastructure) {
        var nodes = infrastructure.listNodes();
        var agents = new ConcurrentLinkedQueue<ExperimentalAgent>();

        nodes.forEach(node -> {
            if (!(boolean)node.getProperty("hasAgent").orElse(false)) return;
            var agent = this.fromNode(infrastructure, node);
            if (agent != null) agents.add((ExperimentalAgent) agent);
        });
        return agents;
    }


    public IAgent fromNode(Infrastructure infrastructure, InfrastructureNode node) {
        var agent = this.featureSet.getAgent(infrastructure, node);
        return agent;
    }
}
