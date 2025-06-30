package tech.jorn.adrian.experiment;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.events.EventManager;
import tech.jorn.adrian.core.messages.EventMessage;
import tech.jorn.adrian.core.messages.Message;
import tech.jorn.adrian.experiment.eventNode;

import java.util.LinkedList;
import java.util.PriorityQueue;

public class GlobalQueue {
    public long simulatedTime = 0;

     LinkedList<eventNode> eventQueue = new LinkedList<>();

    protected final Logger log = LogManager.getLogger(EventManager.class);

     public GlobalQueue(EventManager eventManager) {

     }


    public void add(Event event, long duration) {
        //Dauer der Aufgabe wird übergeben und Endzeitpunkt berechnet, nach diesem wird die Liste sortiert/der Knoten eingefügt
        long finishedAt = simulatedTime + duration;
        eventNode node = new eventNode(event, finishedAt);
        if(eventQueue.isEmpty()) {
            eventQueue.add(node);
        }
        else {
            //Position zum Einfügen bestimmen
            int index = 0;
            for (tech.jorn.adrian.experiment.eventNode eventNode : eventQueue) {
                if (eventNode.getFinishTime() < finishedAt) {
                    break;
                }
                index++;
            }
            eventQueue.add(index, node);
        }

    }

    public eventNode poll() {
        if(eventQueue.isEmpty()) return null;
        return eventQueue.pollFirst();
    }

    public boolean isEmpty() {
        return eventQueue.poll() == null;
    }

    public void setSimulatedTime(long updatedTime) {
         this.simulatedTime = updatedTime;
    }

    public long getSimulatedTime() {
         return simulatedTime;
    }








         }










