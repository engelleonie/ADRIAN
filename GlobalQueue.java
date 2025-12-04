 package tech.jorn.adrian.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.events.EventManager;
import tech.jorn.adrian.core.messages.EventMessage;
import tech.jorn.adrian.core.messages.Message;
import tech.jorn.adrian.core.EventNode;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

public class GlobalQueue {
    public static long simulatedTime = 0;
    private static final GlobalQueue instance = new GlobalQueue();
    public static GlobalQueue getInstance() {
        return instance;
    }

    private GlobalQueue() {

    }

    // priority queue sorted by earliest finished event, calculated by adding duration to current simulated time
    private final PriorityQueue<EventNode> globalQueue = new PriorityQueue<>(Comparator.comparingLong(EventNode::getFinishTime));

    public static final Logger log = LogManager.getLogger(EventManager.class);

    //adding an event to the queue
    public void offer(Event event, long duration) {

        //don't add event if the same event is already in the queue
        //unnecessary if messages and knowledge is handled correctly
         if (isDuplicate(event)) {
            log.debug("Skipping duplicate event {} for agent {}",
                    event.getClass().getSimpleName(), event.getAgentID());
            return;
        }
        EventNode node = new EventNode<>(event, simulatedTime + duration);
        synchronized(globalQueue) {
            globalQueue.offer(node);
        }
        log.debug("Queued event {} from agent {} with finishTime={}",
                event.getClass().getSimpleName(), event.getAgent().getID(),
                node.getFinishTime());
        log.debug("Agentstate of agent {} : {}", event.getAgent().getID(), event.getAgent().getState());
    }


    public EventNode poll() {
        return globalQueue.poll();
    }
    public EventNode peek() {
        return globalQueue.peek();
    }

    public boolean isEmpty() {
        return globalQueue.isEmpty();
    }
    public long getSimulatedTime() {
        return simulatedTime;
    }
    public void setSimulatedTime(long time) {
        this.simulatedTime = time;
    }

    public List<Event> listQueueItems() {
        return globalQueue.stream()
                .map(EventNode::getEvent)
                .toList();
    }

    public int getSize() {
        return listQueueItems().size();
    }
    public boolean remove(Event event) {
        return globalQueue.removeIf(node -> node.getEvent().equals(event));
    }

    //counting all pending events for an agent
    public int countPendingEventsFor(String agentId) {
        synchronized(globalQueue) {
            return (int) globalQueue.stream()
                    .filter(node -> node.getEvent() != null)
                    .filter(node -> {
                        Event e = node.getEvent();
                        try {
                            var m = e.getClass().getMethod("getAgentID");
                            Object id = m.invoke(e);
                            return agentId.equals(id);
                        } catch (Exception ex) {
                            return false;
                        }
                    })
                    .count();
        }
    }

    //testing if agent has any pending event queued
    private boolean hasAgentId(Event e, String agentID) {
        try {
            var method = e.getClass().getMethod("getAgentID");
            Object id = method.invoke(e);
            return agentID.equals(id);
        } catch (Exception ex) {
            return false;
        }
    }

    //checking if the same event is already queued
     private boolean isDuplicate(Event event) {
        synchronized (globalQueue) {
            return globalQueue.stream().anyMatch(node -> {
                Event e = node.getEvent();
                System.out.println("Agentid event: " + event.getAgentID());
                System.out.println("GetAgentid event: " + event.getAgent().getID());
                System.out.println("Agentid e: " + e.getAgentID());
                System.out.println("GetAgentid e: " + e.getAgent().getID());

                if (!e.getClass().equals(event.getClass())) return false;

                try {
                    var method = e.getClass().getMethod("getAgentID");
                    Object existingId = method.invoke(e);
                    var newMethod = event.getClass().getMethod("getAgentID");
                    Object newId = newMethod.invoke(event);
                    if (!existingId.equals(newId)) return false;

                    // testing if recipient is the same
                    var recMethod = e.getClass().getMethod("getRecipient");
                    Object existingRecipient = recMethod.invoke(e);

                    Object newRecipient = event.getClass().getMethod("getRecipient").invoke(event);

                    return existingRecipient.equals(newRecipient);

                } catch (Exception ex) {
                    return false;
                }
            });
        }
    }



}










