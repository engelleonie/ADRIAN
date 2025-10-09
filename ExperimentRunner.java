package tech.jorn.adrian.experiment;


import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;

import tech.jorn.adrian.agent.AdrianAgent;
import tech.jorn.adrian.agent.NodeRegistry;
import tech.jorn.adrian.agent.controllers.KnowledgeController;
import tech.jorn.adrian.agent.controllers.RiskController;
import tech.jorn.adrian.agent.controllers.SleepController;
import tech.jorn.adrian.agent.controllers.SystemController;
import tech.jorn.adrian.agent.events.*;
import tech.jorn.adrian.core.EventNode;
import tech.jorn.adrian.core.GlobalQueue;
import tech.jorn.adrian.core.agents.AgentState;
import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.controllers.IController;
import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.events.EventManager;
import tech.jorn.adrian.core.graphs.MermaidGraphRenderer;
import tech.jorn.adrian.core.graphs.base.GraphLink;
import tech.jorn.adrian.core.graphs.base.INode;
import tech.jorn.adrian.core.graphs.infrastructure.Infrastructure;
import tech.jorn.adrian.core.graphs.infrastructure.InfrastructureEntry;
import tech.jorn.adrian.core.graphs.infrastructure.InfrastructureNode;
import tech.jorn.adrian.core.graphs.risks.AttackGraphEntry;
import tech.jorn.adrian.core.graphs.risks.AttackGraphLink;
import tech.jorn.adrian.core.messages.EventMessage;
import tech.jorn.adrian.core.observables.EventDispatcher;
import tech.jorn.adrian.core.observables.FlagDispatcher;
import tech.jorn.adrian.core.observables.SubscribableValueEvent;
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

public class ExperimentRunner {
    private static int tick = 0;
    public static long start = System.currentTimeMillis();
    // private static Timer updateTimer = new Timer();

    //public static long simulatedTime = 0;
    static LinkedList<EventNode> eventQueue = new LinkedList<>();

    private static final GlobalQueue globalQueue = GlobalQueue.getInstance();



    private static final EventManager eventManager = null;

    public static void main(String[] args) throws InterruptedException {


        String[] param = new String[3];
        param[0] = "complex-infra.yml";
        param[1] = "no-change";
        param[2] = "auctioning";

        System.out.println(Arrays.stream(args).collect(Collectors.joining(", ")));
        var file = param[0];
        var infrastructure = InfrastructureLoader.loadFromYaml(file);
        infrastructure.getNodes().forEach(n -> {
            var neighbours = infrastructure.getNeighbours(n);
            System.out.println("  " + n.getID() + " -> " +
                    neighbours.stream().map(x -> x.getID()).toList());
        });
        var messageDispatcher = new EventDispatcher<Envelope>();
        var features = getFeatureSet(param[2], messageDispatcher);
        var agentFactory = new AgentFactory(features, globalQueue);

        var scenario = getScenario(param[1], infrastructure, messageDispatcher, (node) -> agentFactory.fromNode(infrastructure, node));
        String config = param[0].substring(0, param[0].length() - 4) + "_" + param[1] + "_" + param[2];

        runTest(infrastructure, features, scenario, config, globalQueue);
    }

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

    public static void runTest(Infrastructure infrastructure, FeatureSet featureSet, Scenario scenario, String config, GlobalQueue globalQueue) {
        var log = LogManager.getLogger(ExperimentRunner.class);

        renderInfrastructure(infrastructure);

        var startTime = new Date().getTime();

        var agentFactory = new AgentFactory(featureSet, globalQueue);
        var metricCollector = new MetricCollector(infrastructure);

        log.debug("Creating agents");

        var agents = agentFactory.fromInfrastructure(infrastructure);
        AtomicInteger sleepingAgents = new AtomicInteger(0);
        int totalAgents = agents.size();

        for (AdrianAgent agent : agents) {
            agent.getEventManager().registerEventHandler(AgentSleepingEvent.class, event -> {
                int count = sleepingAgents.incrementAndGet();
                log.info("Agent {} sleeping ({}/{})", agent.getID(), count, totalAgents);

                if (count == totalAgents - 1) {
                    Optional<ExperimentalAgent> lastAwake = agents.stream()
                            .filter(a -> a.getState() != AgentState.Sleeping)
                            .findFirst();

                    lastAwake.ifPresent(a -> {
                        if (a.getFailedSearches() < 3) {
                            log.info("Only one agent left awake ({}). Emitting one IdentifyRiskEvent to let it progress toward sleep.", a.getID());
                            GlobalQueue.getInstance().offer(
                                    new IdentifyRiskEvent(a.getConfiguration().getNodeID()),
                                    GlobalQueue.getInstance().getSimulatedTime() + 1
                            );
                        } else {
                            log.debug("Last awake agent {} already has failedSearches >= 3 ({}), no forced event.", a.getID(), a.getFailedSearches());
                        }
                    });
                }

                if (count == totalAgents) {
                    log.info("All Agents asleep, finishing simulation.");
                    GlobalQueue.getInstance().offer(
                            new FinishScenarioEvent(scenario.finishedDispatcher()),
                            GlobalQueue.getInstance().getSimulatedTime()
                    );
                }
            });

            agent.getEventManager().registerEventHandler(AgentWakeUpEvent.class, event -> {
                int count = sleepingAgents.decrementAndGet();
                log.info("Agent {} woke up ({} of {} asleep)",
                        event.getAgentID(), count, totalAgents);
            });
        }

        List<ExperimentalAgent> agentList = new ArrayList<>(agents);

        AtomicInteger shutdownCount = new AtomicInteger(0);

        Runnable onFinished = () ->  {

            log.info("Finished scenario in {}ms", new Date().getTime() - startTime);

            log.info("Stopping {} agents", agentList.size());

            System.out.println(6);

            try {
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

        for (AdrianAgent agent : agents) {
            agent.onStateChange().subscribe(state -> {
                if (state == AgentState.Shutdown) {
                    int current = shutdownCount.incrementAndGet();
                    log.info("Agent {} went to Shutdown ({}/{})",
                            agent.getConfiguration().getNodeID(),
                            current, totalAgents);

                    if (current == totalAgents) {
                        log.info("All agents are Shutdown → calling onFinished");
                        onFinished.run();
                    }
                }
            });
        }

        agents.forEach(metricCollector::listenToAgent);
        scenario.onNewAgent().subscribe(agent -> {
            metricCollector.listenToAgent(agent);
            agents.add(agent);
            agentList.add(agent);
        });

        for (ExperimentalAgent agent : agentList) {
            new SystemController(agent.getEventManager(), agent.onStateChange());
        }

        scenario.onFinished().subscribe(onFinished);

        log.debug("Starting agents");

        for (INode node : infrastructure.listNodes()) {
            NodeRegistry.getInstance().registerNode(node);
            System.out.println("Node registered: " + node.getID());
        }
        //changes state of each agent to idle, triggers initial knowledge sharing
        agents.forEach(AdrianAgent::startReady);
        agents.forEach(AdrianAgent::startIdle);

        //scenario.scheduleEvents(agents);

        List<AdrianAgent> adrianAgents = new ArrayList<>(agentList);

        Runnable onAllSleeping = () -> {
            log.info("All agents asleep, finishing simulation.");
            var finishEvent = new FinishScenarioEvent(scenario.finishedDispatcher());
            GlobalQueue.getInstance().offer(finishEvent, GlobalQueue.getInstance().getSimulatedTime());
        };

        long interval = 10;
        EventDispatcher<Void> tickDispatcher = new EventDispatcher<>();
        tickDispatcher.subscribe((Void v) -> {
            agents.forEach(agent -> {
                agent.checkSleep();
                agent.markActive();
            });
            metricCollector.updateInterval(agents);
            log.debug("Metrics updated at simTime={}", GlobalQueue.simulatedTime);
            boolean allSleeping = agents.stream().allMatch(a -> a.getState() == AgentState.Sleeping);
            if (!allSleeping) {
                GlobalQueue.getInstance().offer(
                        new MetricTickEvent(GlobalQueue.getInstance().getSimulatedTime() + interval, tickDispatcher),
                        GlobalQueue.getInstance().getSimulatedTime() + interval
                );
            }
        });

        /* for (ExperimentalAgent agent : agents) {
            List<IController> controllers = agent.getControllers();

            for (IController controller : controllers) {
                if (controller instanceof KnowledgeController) {
                    ((KnowledgeController) controller).shareKnowledge();
                }
            }
        } */

         for (ExperimentalAgent agent : agents) {
             var event = new IdentifyRiskEvent(agent.getConfiguration().getNodeID());
             GlobalQueue.getInstance().offer(event, 5);
         }

        GlobalQueue queue = GlobalQueue.getInstance();

        queue.offer(new MetricTickEvent(queue.getSimulatedTime() + interval, tickDispatcher), queue.getSimulatedTime() + interval);

        QueueExecutor executor = new QueueExecutor(globalQueue, agentList, 10000);
        executor.execute();

        renderInfrastructure(infrastructure);

        List<Event> events = GlobalQueue.getInstance().listQueueItems();
        events.forEach(event -> log.debug("Pending: {}", event));
    }

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
            System.err.println("SOMETHING WENT WRONG OUTPUTTING GRAPH " + e.toString());
            throw new RuntimeException(e);
        }
    }

    /* public static TimerTask createUpdateTimerTask(Queue<ExperimentalAgent> agents, MetricCollector metricCollector) {

        return new TimerTask() {
            // ??
            Logger log = LogManager.getLogger(ExperimentRunner.class);

            @Override
            public void run() {
                //2 Zeilen nötig??
                final Thread thread = Thread.currentThread();
                thread.setPriority(Thread.MAX_PRIORITY);

                // log.debug("Updating metrics");
                metricCollector.updateInterval(agents);
                tick++;

                thread.setPriority(Thread.NORM_PRIORITY);
            }
        };
    } */

    // debugging
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

