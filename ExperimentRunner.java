package tech.jorn.adrian.experiment;


import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;

import tech.jorn.adrian.agent.AdrianAgent;
import tech.jorn.adrian.agent.NodeRegistry;
import tech.jorn.adrian.agent.controllers.KnowledgeController;
import tech.jorn.adrian.agent.controllers.RiskController;
//import tech.jorn.adrian.agent.controllers.SystemController;
import tech.jorn.adrian.agent.events.*;
import tech.jorn.adrian.core.GlobalQueue;
import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.controllers.IController;
import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.graphs.MermaidGraphRenderer;
import tech.jorn.adrian.core.graphs.base.GraphLink;
import tech.jorn.adrian.core.graphs.base.INode;
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
import tech.jorn.adrian.experiment.messages.InMemoryBroker;
import tech.jorn.adrian.experiment.scenarios.GrowingInfrastructureScenario;
import tech.jorn.adrian.experiment.scenarios.IntroduceRiskScenario;
import tech.jorn.adrian.experiment.scenarios.LargeScenario;
import tech.jorn.adrian.experiment.scenarios.MixedScenario;
import tech.jorn.adrian.experiment.scenarios.NoChangeScenario;
import tech.jorn.adrian.experiment.scenarios.Scenario;
import tech.jorn.adrian.experiment.scenarios.UnstableInfrastructureScenario;
import java.util.Random;

public class ExperimentRunner {
    private static int tick = 0;
    public static long start = System.currentTimeMillis();
    private static final GlobalQueue globalQueue = GlobalQueue.getInstance();
    private static int knowledgeCount = 0;
    private static Random rand = new Random();

    public static void main(String[] args) throws InterruptedException {

        //input-parameters for each simulation-run
        String[] param = new String[3];
        //yml file containing network infrastructure
        param[0] = "simple.yml";
        //scenario (options in getScenario() below)
        param[1] = "no-change";
        //featureSet: local, knowledge-sharing or auctioning
        param[2] = "auctioning";



        System.out.println(Arrays.stream(args).collect(Collectors.joining(", ")));
        var file = param[0];
        var infrastructure = InfrastructureLoader.loadFromYaml(file);
        //debugging to check if infrastructure was initialized correctly
        infrastructure.getNodes().forEach(n -> {
            var neighbours = infrastructure.getNeighbours(n);
            System.out.println("  " + n.getID() + " -> " +
                    neighbours.stream().map(x -> x.getID()).toList());
        });


        //responsible for communication between Agents
        var messageDispatcher = new EventDispatcher<Envelope>();
        // initializes components necessary to support chosen featureSet
        var features = getFeatureSet(param[2], messageDispatcher);
        //create agentFactory instance, necessary to create agents later
        var agentFactory = new AgentFactory(features);


        var scenario = getScenario(param[1], infrastructure, messageDispatcher, (node) -> agentFactory.fromNode(infrastructure, node));
        //creating CSV-name for current simulation
        String config = param[0].substring(0, param[0].length() - 4) + "_" + param[1] + "_" + param[2];

        //starting agent initialization first, simulation-loop is triggered at the end of runTest()
        runTest(infrastructure, scenario, config, globalQueue, agentFactory);
    }


    // initializes chosen scenario
    public static Scenario getScenario(String input, Infrastructure infrastructure, EventDispatcher<Envelope> messageDispatcher, Function<InfrastructureNode, IAgent> agentFactory) {
        switch (input) {
            case "large": return new LargeScenario(infrastructure, messageDispatcher, agentFactory);
            case "risk-introduction": return new IntroduceRiskScenario(infrastructure, messageDispatcher);
            case "growing": return new GrowingInfrastructureScenario(infrastructure, messageDispatcher, agentFactory);
            case "unstable": return new UnstableInfrastructureScenario(infrastructure, messageDispatcher, agentFactory);
            case "mixed": return new MixedScenario(infrastructure, messageDispatcher);
            case "no-change":
            default:
                return new NoChangeScenario(infrastructure, messageDispatcher);
        }
    }

    // initializes chosen featureSet
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

    public static void runTest(Infrastructure infrastructure, Scenario scenario, String config, GlobalQueue globalQueue, AgentFactory agentFactory) {
        var log = LogManager.getLogger(ExperimentRunner.class);

        renderInfrastructure(infrastructure);

        var startTime = new Date().getTime();

        //var agentFactory = new AgentFactory(featureSet, globalQueue);
        var metricCollector = new MetricCollector(infrastructure);

        log.debug("Creating agents");

        //initializes ExperimentalAgents
        var agents = agentFactory.fromInfrastructure(infrastructure);
        //additional lists containing all agents
        List<ExperimentalAgent> agentList = new ArrayList<>(agents);
        agents.forEach(agent -> {
            NodeRegistry.getInstance().registerAgent(agent);
        });


        agents.forEach(metricCollector::listenToAgent);
        //adding agent to data structures in case one is created during simulation
        scenario.onNewAgent().subscribe(agent -> {
            metricCollector.listenToAgent(agent);
            agents.add(agent);
            agentList.add(agent);
        });

        //called when QueueExecutor calls FinishScenarioEvent
        Runnable onFinished = () ->  {
            log.info("Finished scenario in {}ms", new Date().getTime() - startTime);
            log.info("Stopping {} agents", agentList.size());

            List<Event> remainingEvents = globalQueue.listQueueItems();
            if (remainingEvents.isEmpty()) {
                log.info("No remaining events in the global queue.");
            } else {
                log.info("Remaining events in the global queue ({}):", remainingEvents.size());
                remainingEvents.forEach(e -> log.info("  - {}", e.getClass().getSimpleName() + " [" + e + "]"));
            }

            try {
                //final update to metrics.csv containing all metrics of the current simulation run
                log.debug("Writing measures");
                metricCollector.updateInterval(agents);
                metricCollector.writeToCSV(agents, new Date().getTime() - startTime, config);

                renderInfrastructure(infrastructure);
            } catch (IOException e) {
                log.error("Could not write measures");
            }
            long end = System.currentTimeMillis();
            long runtimeSec = (end - start) / 1000;
            System.out.println("Physical time: " + runtimeSec);
            System.out.println("simulation runtime: " + GlobalQueue.simulatedTime + " ms");

            System.out.println("Total messages sent: " + InMemoryBroker.getMessageCount());
            System.out.println("identifyRisk() was called: " + RiskController.getIdentifyRiskCallCount() + " times");

            System.out.println("executed");

            System.exit(0);
        };

        scenario.onFinished().subscribe(onFinished);

        log.debug("Starting agents");

        for (INode node : infrastructure.listNodes()) {
            NodeRegistry.getInstance().registerNode(node);
            System.out.println("Node registered: " + node.getID());
        }
        //changes state of each agent to idle
        agents.forEach(AdrianAgent::startReady);
        agents.forEach(AdrianAgent::startIdle);




        //creates final event once the global queue is empty, stopping the simulation
        Runnable onQueueEmpty = () -> {
            log.info("All agents idle, finishing simulation.");
            // finishEvent doesn't need a specific agent, triggers end of simulation
            var finishEvent = new FinishScenarioEvent(scenario.finishedDispatcher(), agents.peek());
            GlobalQueue.getInstance().offer(finishEvent, GlobalQueue.getInstance().getSimulatedTime());
        };


        //creates initial ShareKnowledgeEvents for each agent
          for (ExperimentalAgent agent : agents) {
            List<IController> controllers = agent.getControllers();

            for (IController controller : controllers) {
                if (controller instanceof KnowledgeController) {
                    //shareKnowledge creates new ShareKnowledgeEvent but also triggers immediate knowledge sharing, causing issues later on
                    // as knowledge is not updated properly by later ShareKnowledgeEvents
                    // new logic does not create a ShareKnowledgeEvent, instead creates SendMessageEvents immediately
                    ((KnowledgeController) controller).shareKnowledge();
                    //debugging
                    knowledgeCount++;
                    System.out.println("knowledgeCount: " + knowledgeCount);
                }
            }
        }

          //creates initial IdentifyRiskEvent
         for (ExperimentalAgent agent : agents) {
             var event = new IdentifyRiskEvent(agent);
             log.debug("agent: {}, eventid: {}", agent.getID(), event.getAgentID());
             GlobalQueue.getInstance().offer(event, rand.nextInt(20));
         }

         //creates executor and starts simulation-loop
        QueueExecutor executor = new QueueExecutor(agentList, metricCollector, agents, onQueueEmpty, onFinished);
         //calls Event-Loop, loop has while(true) condition, only stops once FinishScenarioEvent is executed which is triggered once a hard timeout
         // is reached or when the globalQueue is empty
        executor.execute();

        renderInfrastructure(infrastructure);

        //lists remaining items in global queue, prints them at the end of the simulation
        List<Event> events = GlobalQueue.getInstance().listQueueItems();
        events.forEach(event -> log.debug("Pending: {}", event));
    }

    //creates mmd-Graph containing applied proposals
    private static void renderInfrastructure(Infrastructure infrastructure) {
        var filename = String.format("./graphs/infrastructure-%d.mmd", System.currentTimeMillis() - start);
        try {
            var writer = new FileWriter(filename);
            var graphRender = new MermaidGraphRenderer<InfrastructureEntry<?>, GraphLink<InfrastructureEntry<?>>>();
            var mmdGraph = graphRender.render(infrastructure);
            writer.write("%% " + tick * 5000 + "\n");
            writer.write(mmdGraph);
            writer.close();
        } catch (IOException e) {
            System.err.println("SOMETHING WENT WRONG OUTPUTTING GRAPH " + e);
            throw new RuntimeException(e);
        }
    }

    // debugging to follow auction behavior
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

