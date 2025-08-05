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
        // Konstruktor privat machen, damit niemand neue Instanzen erzeugt
    }

    private final PriorityQueue<EventNode> globalQueue = new PriorityQueue<>(Comparator.comparingLong(EventNode::getFinishTime));

    protected static final Logger log = LogManager.getLogger(EventManager.class);

    public void offer(Event event, long duration) {

        EventNode node = new EventNode<>(event, simulatedTime + duration);
        globalQueue.offer(node);
        log.debug("Offering event to queue: {}", event.getClass().getSimpleName());
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

}










