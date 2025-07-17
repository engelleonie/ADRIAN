package tech.jorn.adrian.core;

import tech.jorn.adrian.core.events.Event;

public class EventNode<E extends Event> {
    public E event;
    public long finishTime;

    public EventNode(E event, long finishTime) {
        this.event = event;
        this.finishTime = finishTime;
    }

    public long getFinishTime() {
        return this.finishTime;
    }

    public E getEvent() {
        return this.event;
    }

}
