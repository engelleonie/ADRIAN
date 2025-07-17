package tech.jorn.adrian.experiment;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.jorn.adrian.core.EventNode;
import tech.jorn.adrian.core.GlobalQueue;
import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.events.EventManager;

import java.util.List;


public class QueueExecutor {
    private static final Logger log = LogManager.getLogger(QueueExecutor.class);

    private GlobalQueue globalQueue;
    private  List<EventManager> eventManagers;
    private long maxSimTime;

    public QueueExecutor(GlobalQueue globalQueue, List<EventManager> eventManagers, long maxSimTime) {
        this.globalQueue = globalQueue;
        this.eventManagers = eventManagers;
        this.maxSimTime = maxSimTime;
    }

    public void execute() {
            log.info("start execution");
            while (!globalQueue.isEmpty() && globalQueue.getSimulatedTime() < maxSimTime) {
                log.info("in while");
                EventNode node = globalQueue.poll();

                if (node == null) break;

                log.info("after break");
                Event event = node.getEvent();
                globalQueue.setSimulatedTime(node.getFinishTime());

                if (event == null) continue;

                 log.debug("SimTime {}: Processing event {}", globalQueue.getSimulatedTime(), event.getClass().getSimpleName());

                // search for matching eventHandler
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



