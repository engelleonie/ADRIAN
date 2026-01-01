package tech.jorn.adrian.experiment;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Node;
import tech.jorn.adrian.agent.AdrianAgent;
import tech.jorn.adrian.agent.events.*;
import tech.jorn.adrian.core.EventNode;
import tech.jorn.adrian.core.GlobalQueue;
import tech.jorn.adrian.core.agents.AgentState;
import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.events.EventManager;
import tech.jorn.adrian.experiment.instruments.ExperimentalAgent;

import java.util.*;
import java.util.List;


public class QueueExecutor {
    private static final Logger log = LogManager.getLogger(QueueExecutor.class);
    private static final GlobalQueue globalQueue = GlobalQueue.getInstance();
    private final MetricCollector metricCollector;
    private Queue<ExperimentalAgent> agentQueue;
    boolean finishEventTriggered = false;
    private final Runnable onQueueEmpty;

    private final Map<String, EventManager> managersByAgentId;
    private final Map<String, AdrianAgent> agentsById;
    private final Runnable onFinished;

    public QueueExecutor(List<ExperimentalAgent> agents, MetricCollector metricCollector, Queue<ExperimentalAgent> agentQueue, Runnable onQueueEmpty,
                         Runnable onFinished) {

        this.managersByAgentId = new HashMap<>();
        this.agentsById = new HashMap<>();
        for (ExperimentalAgent agent : agents) {
            managersByAgentId.put(agent.getID(), agent.getEventManager());
            agentsById.put(agent.getID(), agent);
        }
        this.metricCollector = metricCollector;
        this.agentQueue = agentQueue;
        this.onQueueEmpty = onQueueEmpty;
        this.onFinished = onFinished;
    }

    //called from ExperimentRunner, controls entire event-loop
    public void execute() {
        long startTime = System.currentTimeMillis();
        log.info("Start simulation");
        while (true ) {

            //triggers finalization of simulation in case the queue is empty
            if (!finishEventTriggered && globalQueue.isEmpty()) {
                log.info("Queue is empty, inserting FinishScenarioEvent");
                onQueueEmpty.run();
                finishEventTriggered = true;
            }
            EventNode node = globalQueue.poll();

            if (node == null) {
                continue;
            }

            Event event = node.getEvent();
            //setting simulated time to when current event will be finished
            globalQueue.setSimulatedTime(node.getFinishTime());

            if (event == null) continue;

            boolean handled = false;

            //executing metric updates or end of simulation manually
            if (event instanceof MetricTickEvent || event instanceof FinishScenarioEvent) {
                event.trigger();
            }




            else {
                String agentId = null;
                if (event instanceof IdentifyRiskEvent) {
                    agentId = event.getAgent().getID();
                    AdrianAgent agent = agentsById.get(agentId);
                    log.debug("Agent {} agentstate: {}", agentId, agent);
                    if (agent.getState() == AgentState.Searching || agent.getState() == AgentState.Auctioning || agent.getState() == AgentState.Migrating) {
                        log.debug("Agent {} is currently handling a risk, skipping IdentifyRiskEvent", agentId);
                        continue;
                    }

                } else {
                    for (String id : managersByAgentId.keySet()) {
                        EventManager m = managersByAgentId.get(id);
                        if (m.canHandle(event)) {
                            agentId = id;
                            break;
                        }
                    }
                }

                if (agentId != null || event.getAgent() != null) {
                    if (event.getAgent() != null) {
                        agentId = event.getAgent().getID();
                    }

                    AdrianAgent agent = agentsById.get(agentId);
                    EventManager manager = managersByAgentId.get(agentId);

                    if (agent == null || manager == null) {
                        log.warn("Agent or manager not found for event {} of {}", event.getClass().getSimpleName(), agentId);
                        continue;
                    }

                    if (event instanceof IdentifyRiskEvent &&
                            (agent.getState() == AgentState.Searching ||
                                    agent.getState() == AgentState.Auctioning ||
                                    agent.getState() == AgentState.Migrating)) {
                        log.debug("Agent {} busy, skipping event {}", agentId, event.getClass().getSimpleName());
                        continue;
                    }

                    log.debug("Agent {} processes {} at simTime={}", agentId, event.getClass().getSimpleName(),
                            globalQueue.getSimulatedTime());

                    System.out.println("Manager processing: " + manager.getClass());
                    manager.processEvent(event);
                    handled = true;

                    metricCollector.updateInterval(agentQueue);
                }

                if (!handled) {
                    log.warn("Event {} was not handled", event.getClass().getSimpleName());
                }

                //hard timeout for simulation
                if (System.currentTimeMillis() - startTime >= 180000) {
                    log.warn("Simulation timed out after 3 minutes");
                    onFinished.run();
                    break;
                }
            }
        }

    }

}



