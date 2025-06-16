package tech.jorn.adrian.experiment;


import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.graphs.MermaidGraphRenderer;
import tech.jorn.adrian.core.graphs.base.GraphLink;
import tech.jorn.adrian.core.graphs.infrastructure.Infrastructure;
import tech.jorn.adrian.core.graphs.infrastructure.InfrastructureEntry;
import tech.jorn.adrian.core.graphs.infrastructure.InfrastructureNode;
import tech.jorn.adrian.core.graphs.risks.AttackGraphEntry;
import tech.jorn.adrian.core.graphs.risks.AttackGraphLink;
import tech.jorn.adrian.core.observables.EventDispatcher;
import tech.jorn.adrian.core.risks.RiskReport;
import tech.jorn.adrian.experiment.features.AgentFactory;
import tech.jorn.adrian.experiment.features.FeatureSet;
import tech.jorn.adrian.experiment.features.FullFeatureSet;
import tech.jorn.adrian.experiment.features.NoAuctionFeatureSet;
import tech.jorn.adrian.experiment.features.NoCommunicationFeatureSet;
import tech.jorn.adrian.experiment.instruments.ExperimentalAgent;
import tech.jorn.adrian.experiment.messages.Envelope;
import tech.jorn.adrian.experiment.scenarios.GrowingInfrastructureScenario;
import tech.jorn.adrian.experiment.scenarios.IntroduceRiskScenario;
import tech.jorn.adrian.experiment.scenarios.LargeScenario;
import tech.jorn.adrian.experiment.scenarios.MixedScenario;
import tech.jorn.adrian.experiment.scenarios.NoChangeScenario;
import tech.jorn.adrian.experiment.scenarios.Scenario;
import tech.jorn.adrian.experiment.scenarios.UnstableInfrastructureScenario;

public class ExperimentRunner {
    private static int tick = 0;
    private static Timer updateTimer = new Timer();

    public static void main(String[] args) throws InterruptedException {
        System.out.println(Arrays.stream(args).collect(Collectors.joining(", ")));
        var file = args[0];
        var infrastructure = InfrastructureLoader.loadFromYaml(file);
        var messageDispatcher = new EventDispatcher<Envelope>();
        var features = getFeatureSet(args[2], messageDispatcher);
        var agentFactory = new AgentFactory(features);
        var scenario = getScenario(args[1], infrastructure, messageDispatcher, (node) -> agentFactory.fromNode(infrastructure, node));

        runTest(infrastructure, features, scenario);
    }

    public static Scenario getScenario(String input, Infrastructure infrastructure, EventDispatcher<Envelope> messageDispatcher, Function<InfrastructureNode, IAgent> agentFactory) {
        switch (input) {
            case "large": return new LargeScenario(infrastructure, messageDispatcher, agentFactory);
            case "risk-introduction": return new IntroduceRiskScenario(infrastructure, messageDispatcher);
            case "growing": return new GrowingInfrastructureScenario(infrastructure, messageDispatcher, agentFactory);
            case "unstable": return new UnstableInfrastructureScenario(infrastructure, messageDispatcher, agentFactory);
            case "mixed": return new MixedScenario(infrastructure, messageDispatcher);
            case "no-chance":
            default:
                return new NoChangeScenario(infrastructure, messageDispatcher);
        }
    }

    public static FeatureSet getFeatureSet(String input, EventDispatcher<Envelope> messageDispatcher) {
        switch(input) {
            case "knowledge-sharing":
                return new NoAuctionFeatureSet(messageDispatcher);
            case "local":
                return new NoCommunicationFeatureSet();
            case "auctioning":
            case "full":
            default:
                return new FullFeatureSet(messageDispatcher);
        }
    }


    public static void runTest(Infrastructure infrastructure, FeatureSet featureSet, Scenario scenario) {
        var log = LogManager.getLogger(ExperimentRunner.class);

        renderInfrastructure(infrastructure);

        var startTime = new Date().getTime();

        var agentFactory = new AgentFactory(featureSet);
        var metricCollector = new MetricCollector(infrastructure);

        log.debug("Creating agents");
        var agents = agentFactory.fromInfrastructure(infrastructure);
        
        scenario.onNewAgent().subscribe(agent -> {
            metricCollector.listenToAgent(agent);
            agents.add(agent);
        });
        agents.forEach(agent -> metricCollector.listenToAgent(agent));

        scenario.scheduleEvents(agents);

        var task = createUpdateTimerTask(agents, metricCollector);
        updateTimer.scheduleAtFixedRate(task, TimeUnit.SECONDS.toMillis(0), TimeUnit.SECONDS.toMillis(5));
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        log.debug("Starting agents");

        var scheduler = Executors.newScheduledThreadPool(agents.size());

        Runnable onFinished = () ->  {
            log.info("Finished scenario in {}ms", new Date().getTime() - startTime);
            
            scheduler.shutdownNow();
            agents.forEach(a -> a.stop());

            try {
                log.debug("Writing measures");
                metricCollector.updateInterval(agents);
                metricCollector.writeToCSV(agents, new Date().getTime() - startTime);
                renderInfrastructure(infrastructure);
            } catch (IOException e) {
                log.error("Could not write measures");
            }

            System.exit(0);
        };
        scenario.onFinished().subscribe(onFinished);

        
        agents.forEach(a -> {
            scheduler.submit(a::start);
        });
    }

    public static TimerTask createUpdateTimerTask(Queue<ExperimentalAgent> agents, MetricCollector metricCollector) {
        
        return new TimerTask() {
            Logger log = LogManager.getLogger(ExperimentRunner.class);

            @Override
            public void run() {
                final Thread thread = Thread.currentThread();
                thread.setPriority(Thread.MAX_PRIORITY);

                // log.debug("Updating metrics");
                metricCollector.updateInterval(agents);
                tick++;

                thread.setPriority(Thread.NORM_PRIORITY);
            }
        };
    }

    private static void renderInfrastructure(Infrastructure infrastructure) {
        var filename = String.format("./graphs/infrastructure-%d.mmd", tick * 5000);
        try {
            var writer = new FileWriter(filename);
            var graphRender = new MermaidGraphRenderer<InfrastructureEntry<?>, GraphLink<InfrastructureEntry<?>>>();
            var mmdGraph = graphRender.render(infrastructure);
            writer.write("%% " + tick * 5000 + "\n");
            writer.write(mmdGraph);
            writer.close();
        } catch (IOException e) {
            System.err.println("SOMETHING WENT WRONG OUTPUTTING GRAPH " + e.toString());
            throw new RuntimeException(e);
        }
    }

    private static void renderAuction(String id, RiskReport report) {
        var filename = String.format("./graphs/auction-%s.mmd", id);
        try {
            var writer = new FileWriter(filename);
            var graphRender = new MermaidGraphRenderer<AttackGraphEntry<?>, AttackGraphLink<AttackGraphEntry<?>>>();
            var mmdGraph = graphRender.render(report.graph());
            writer.write("%% " + report.toString() + "\n");
            writer.write(mmdGraph);
            writer.close();
        } catch (IOException e) {
            System.err.println("SOMETHING WENT WRONG OUTPUTTING GRAPH " + e.toString());
            throw new RuntimeException(e);
        }
    }
}

