package tech.jorn.adrian.experiment;

import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.messages.EventMessage;
import tech.jorn.adrian.core.messages.Message;

public class eventNode<E extends Event> {
    public E event;
    public long finishTime;

    public eventNode(E event, long finishTime) {
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
