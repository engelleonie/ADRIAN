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

    private int maxEvents = 1000000;

    public QueueExecutor(GlobalQueue globalQueue, List<EventManager> eventManagers, long maxSimTime) {
        this.globalQueue = globalQueue;
        this.eventManagers = eventManagers;
        this.maxSimTime = maxSimTime;
    }

    public void execute() {
            log.info("start execution");
            while (globalQueue.getSimulatedTime() < maxSimTime && !globalQueue.isEmpty()) {
                EventNode node = globalQueue.poll();

                if (node == null) {
                    System.out.println(1);
                    log.debug("No events to process, waiting...");
                    continue;
                }

                Event event = node.getEvent();
                System.out.println(2);

                if (event == null) {
                    System.out.println(3);
                    EventNode next = globalQueue.peek();
                    if (next != null && next.getFinishTime() > globalQueue.getSimulatedTime()) {
                        // Skip nach vorne: simTime erhöhen
                        globalQueue.setSimulatedTime(next.getFinishTime());
                        System.out.println(4);
                        continue;
                    } else {
                        System.out.println(5);
                        break; // keine Events mehr
                    }
                }
                globalQueue.setSimulatedTime(node.getFinishTime());


                 log.debug("SimTime {}: Processing event {}", globalQueue.getSimulatedTime(), event.getClass().getSimpleName());

                // search for matching eventHandler
                boolean handled = false;
                for (EventManager manager : eventManagers) {
                    if (manager.canHandle(event)) {
                        manager.processEvent(event);
                        handled = true;
                        log.info("Event processed successfully");
                        //maxEvents--;

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



