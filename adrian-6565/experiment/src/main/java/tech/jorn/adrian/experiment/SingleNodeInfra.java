package tech.jorn.adrian.experiment;


import tech.jorn.adrian.core.observables.EventDispatcher;
import tech.jorn.adrian.experiment.features.AgentFactory;
import tech.jorn.adrian.experiment.features.FullFeatureSet;
import tech.jorn.adrian.experiment.instruments.ExperimentalAgent;
import tech.jorn.adrian.experiment.messages.Envelope;
import tech.jorn.adrian.experiment.scenarios.NoChangeScenario;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;



public class SingleNodeInfra {
    public static void main(String[] args) throws InterruptedException {

        String[] param = new String[3];
        param[0] = "./testgraph50.yml";
        param[1] = "no-change";
        param[2] = "auctioning";


        var infrastructure = InfrastructureLoader.loadFromYaml("./testgraph50.yml");
        var messageDispatcher = new EventDispatcher<Envelope>();
        var scenario = new NoChangeScenario(infrastructure, messageDispatcher);

        var featureSet = new FullFeatureSet(messageDispatcher);
        var agentFactory = new AgentFactory(featureSet);
        var agents = agentFactory.fromInfrastructure(infrastructure);
        var metricCollector = new MetricCollector(infrastructure);

        //ExperimentRunner.runTest(infrastructure, featureSet, scenario);
        scenario.onNewAgent().subscribe(agent -> {
            metricCollector.listenToAgent(agent);
            agents.add(agent);
        });
        agents.forEach(metricCollector::listenToAgent);
        scenario.scheduleEvents(agents);

        agents.forEach(ExperimentalAgent::start);

        scenario.onFinished().subscribe(() -> {
            System.out.println("Done!");
            System.exit(0);
        });



    }

}

