package tech.jorn.adrian.experiment;

import org.apache.logging.log4j.LogManager;
import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.events.EventManager;
import tech.jorn.adrian.core.messages.EventMessage;
import tech.jorn.adrian.experiment.instruments.ExperimentalAgent;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static tech.jorn.adrian.experiment.ExperimentRunner.eventQueue;

public class QueueExecutor {
    private static final Logger log = (Logger) LogManager.getLogger(QueueExecutor.class);

    private GlobalQueue globalQueue;
    private  List<EventManager> eventManagers;
    private long maxSimTime;

    public QueueExecutor(GlobalQueue globalQueue, List<EventManager> eventManagers, long maxSimTime) {
        this.globalQueue = globalQueue;
        this.eventManagers = eventManagers;
        this.maxSimTime = maxSimTime;
    }

    public void execute() {
        while (!eventQueue.isEmpty()) {
            log.info("start execution");
            while (!globalQueue.isEmpty() && globalQueue.simulatedTime < maxSimTime) {
                var node = globalQueue.poll();

                if (node == null) break;

                Event event = node.getEvent();
                globalQueue.setSimulatedTime(node.getFinishTime());

                if (event == null) continue;

                log.debug("SimTime {}: Processing event {}", globalQueue.getSimulatedTime(), event.getClass().getSimpleName());

                // Suche den passenden EventManager
                boolean handled = false;
                for (EventManager manager : eventManagers) {
                    if (manager.canHandle(event)) {
                        manager.processEvent(event);
                        handled = true;
                        break;
                    }
                }

                if (!handled) {
                    log.warn("No EventManager found for event: {}", event.getClass().getSimpleName());
                }
            }

            log.info("Simulation finished at simTime {} ms");
        }
    }


}
