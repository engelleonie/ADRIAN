package tech.jorn.adrian.experiment;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Node;
import tech.jorn.adrian.agent.controllers.SleepController;
import tech.jorn.adrian.agent.events.FinishScenarioEvent;
import tech.jorn.adrian.agent.events.IdentifyRiskEvent;
import tech.jorn.adrian.core.EventNode;
import tech.jorn.adrian.core.GlobalQueue;
import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.events.EventManager;
import tech.jorn.adrian.experiment.instruments.ExperimentalAgent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class QueueExecutor {
    private static final Logger log = LogManager.getLogger(QueueExecutor.class);

    private GlobalQueue globalQueue;
    private List<EventManager> eventManagers;
    private long maxSimTime;

    private int maxEvents = 1000000;
    private final Map<String, EventManager> managersByAgentId;


    public QueueExecutor(GlobalQueue globalQueue, List<ExperimentalAgent> agents) {
        this.globalQueue = globalQueue;
        //this.eventManagers = eventManagers;
        this.maxSimTime = maxSimTime;
        this.managersByAgentId = new HashMap<>();
        Map<Node, Set<String>> appliedProposals = new HashMap<>();
        for (ExperimentalAgent agent : agents) {
            managersByAgentId.put(agent.getID(), agent.getEventManager());
        }

    }

    public void execute() {
        log.info("Start simulation");

        while (!globalQueue.isEmpty()) {
            EventNode node = globalQueue.poll();
            if (node == null) continue;

            Event event = node.getEvent();
            globalQueue.setSimulatedTime(node.getFinishTime());

            if (event == null) continue;

            boolean handled = false;

            // Spezielle Events wie Szenarioende
            if (event instanceof FinishScenarioEvent finishEvent) {
                finishEvent.trigger();
                handled = true;
            }
            // Agentenspezifische Events
            else if (event instanceof IdentifyRiskEvent riskEvent) {
                String targetAgentId = riskEvent.getAgentID();
                EventManager manager = managersByAgentId.get(targetAgentId);
                if (manager != null && manager.canHandle(event)) {
                    manager.processEvent(event);
                    handled = true;
                }
            }
            // Generische Events: jeder Agent kann sie empfangen
            else {
                for (EventManager m : managersByAgentId.values()) {
                    if (m.canHandle(event)) {
                        m.processEvent(event);
                        handled = true;
                        break;
                    }
                }
            }

            if (!handled) {
                log.warn("Event {} was not handled", event.getClass().getSimpleName());
            }
        }

        log.info("Simulation finished at simTime {}", globalQueue.getSimulatedTime());
    }
}




