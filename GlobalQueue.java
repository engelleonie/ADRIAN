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

    private final PriorityQueue<EventNode> globalQueue = new PriorityQueue<>(Comparator.comparingLong(EventNode::getFinishTime));

    protected static final Logger log = LogManager.getLogger(EventManager.class);
    private boolean acceptNewEvents;

    public void offer(Event event, long duration) {

        if (isDuplicate(event)) {
            log.debug("Skipping duplicate event: {}", event.getClass().getSimpleName());
            return;
        }
        EventNode node = new EventNode<>(event, simulatedTime + duration);
        synchronized(globalQueue) {
            globalQueue.offer(node);
        }
        log.debug("Offering event to queue: {}", event.getClass().getSimpleName());
    }

    public void shutDownNewEvents() {
        acceptNewEvents = false;
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

    public List<Event> getPendingEvents(String agentID) {
        synchronized (globalQueue) {
            return globalQueue.stream()
                    .map(EventNode::getEvent)
                    .filter(e -> e != null && hasAgentId(e, agentID))
                    .toList();
        }
    }

    private boolean hasAgentId(Event e, String agentID) {
        try {
            var method = e.getClass().getMethod("getAgentID");
            Object id = method.invoke(e);
            return agentID.equals(id);
        } catch (Exception ex) {
            return false;
        }
    }


    public boolean hasPendingEventsFor(String agentId) {
        return countPendingEventsFor(agentId) > 0;
    }


    private boolean isDuplicate(Event event) {
        synchronized (globalQueue) {
            return globalQueue.stream().anyMatch(node -> {
                Event e = node.getEvent();
                if (e == null) return false;
                // gleiche Klasse
                if (!e.getClass().equals(event.getClass())) return false;
                // gleiche Agent-ID
                try {
                    var method = e.getClass().getMethod("getAgentID");
                    Object existingId = method.invoke(e);
                    var newMethod = event.getClass().getMethod("getAgentID");
                    Object newId = newMethod.invoke(event);
                    return existingId.equals(newId);
                } catch (Exception ex) {
                    return false;
                }
            });
        }
    }

}










