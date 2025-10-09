package tech.jorn.adrian.experiment;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Node;
import tech.jorn.adrian.agent.AdrianAgent;
import tech.jorn.adrian.agent.controllers.SleepController;
import tech.jorn.adrian.agent.events.*;
import tech.jorn.adrian.core.EventNode;
import tech.jorn.adrian.core.GlobalQueue;
import tech.jorn.adrian.core.agents.AgentState;
import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.events.EventManager;
import tech.jorn.adrian.experiment.instruments.ExperimentalAgent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class QueueExecutor {
    private static final Logger log = LogManager.getLogger(QueueExecutor.class);

    private static final GlobalQueue globalQueue = GlobalQueue.getInstance();
    private List<EventManager> eventManagers;
    private long maxSimTime;

    private int maxEvents = 1000000;
    private final Map<String, EventManager> managersByAgentId;
    private final Map<String, AdrianAgent> agentsById;
    private final int totalAgents;
    private final java.util.concurrent.atomic.AtomicInteger sleepingAgents = new java.util.concurrent.atomic.AtomicInteger(0);
    private final java.util.Set<String> pendingSleepChecks = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public QueueExecutor(GlobalQueue globalQueue, List<ExperimentalAgent> agents, int maxSimTime) {

        //this.eventManagers = eventManagers;
        this.maxSimTime = maxSimTime;
        this.managersByAgentId = new HashMap<>();
        Map<Node, Set<String>> appliedProposals = new HashMap<>();
        for (ExperimentalAgent agent : agents) {
            managersByAgentId.put(agent.getID(), agent.getEventManager());
        }
        this.agentsById = new HashMap<>();
        for (ExperimentalAgent agent : agents) {
            managersByAgentId.put(agent.getID(), agent.getEventManager());
            agentsById.put(agent.getID(), agent);
        }
        this.totalAgents = agents.size();
    }

    public void execute() {

        log.info("Start simulation");
        while (!globalQueue.isEmpty() ) {
            EventNode node = globalQueue.poll();
            if (node == null) {
                continue;
            }

            Event event = node.getEvent();
            globalQueue.setSimulatedTime(node.getFinishTime());
            if (event == null) continue;

            boolean handled = false;

            if (event instanceof MetricTickEvent || event instanceof FinishScenarioEvent) {
                event.trigger();
                handled = true;
            }

            /* if (event instanceof CheckSleepEvent checkEvent) {
                String agentId = checkEvent.getAgentID();
                AdrianAgent agent = agentsById.get(agentId);

                if (agent != null) {
                    int active = agent.getActiveEventCount();
                    var pending = globalQueue.countPendingEventsFor(agentId);

                    if (active == 0 && pending == 0 && agent.getFailedSearches() >= 3) {
                        agent.getEventManager().emit(new AgentSleepingEvent(agentId));
                        log.info("[{}] -> Going to sleep after deferred check", agentId);
                    } else {
                        log.debug("[{}] stays awake (active={}, pending={}, failed={})",
                                agentId, active, pending, agent.getFailedSearches());
                    }
                }
                handled = true;
                continue;
            } */


            else {
                String agentId = null;
                if (event instanceof IdentifyRiskEvent) {
                    agentId = ((IdentifyRiskEvent) event).getAgentID();
                    AdrianAgent agent = agentsById.get(agentId);
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

                if (agentId != null) {
                    EventManager manager = managersByAgentId.get(agentId);
                    AdrianAgent agent = agentsById.get(agentId);

                    if (manager != null && manager.canHandle(event)) {
                        log.debug("Agent {} processes {} at simTime={}", agentId, event.getClass().getSimpleName(),
                                globalQueue.getSimulatedTime());

                        // --- Active Event Counter ---
                        agent.onStartProcessingEvent();
                        try {
                            manager.processEvent(event);
                        } finally {
                            agent.onFinishProcessingEvent();
                        }
                        handled = true;

                        checkSleep(agent);
                    }
                }

                if (!handled) {
                    log.warn("Event {} was not handled", event.getClass().getSimpleName());
                }
            }
        }
        log.info("Simulation finished at simTime {}", globalQueue.getSimulatedTime());
    }

    private void checkSleep(AdrianAgent agent) {
        int failed = agent.getFailedSearches();
        int active = agent.getActiveEventCount();
        int pending = globalQueue.countPendingEventsFor(agent.getID());

        List<Event> pendingTypes = globalQueue.getPendingEvents(agent.getID());
        boolean onlyIdentifyPending = pendingTypes.isEmpty()
                || pendingTypes.stream().allMatch(t -> t.getClass().equals(IdentifyRiskEvent.class));

        if (failed >= 3 && active == 0 && onlyIdentifyPending) {
            long checkTime = globalQueue.getSimulatedTime() + 1;
            log.info("[{}] Agent meets sleep criteria (failed={}, pending={}, active={}, state={})",
                    agent.getConfiguration().getNodeID(), failed, pending, active, agent.getState());

            agent.getEventManager().emit(new AgentSleepingEvent(agent.getID()));
            //globalQueue.offer(new CheckSleepEvent(agent.getID()), checkTime);
            //agent.setState(AgentState.Sleeping);
        }
        else {
            log.debug("[{}] stays awake (failed={}, active={}, pending={}, state={})",
                    agent.getID(), failed, active, pendingTypes, agent.getState());

        }
    }

}




