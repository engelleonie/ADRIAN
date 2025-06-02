package tech.jorn.adrian.core.messages;

import tech.jorn.adrian.core.events.Event;

public class EventMessage<E extends Event> extends Message {
    private final String type;
    private final E event;

    public EventMessage(E event) {
        this.event = event;
        this.type = event.getClass().getSimpleName();
    }

    public String getType() {
        return type;
    }

    public E getEvent() {
        return event;
    }
}
