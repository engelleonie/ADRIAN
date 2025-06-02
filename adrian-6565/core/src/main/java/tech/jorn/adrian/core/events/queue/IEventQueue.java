package tech.jorn.adrian.core.events.queue;

import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.observables.SubscribableEvent;

public interface IEventQueue {
    void enqueue(Event e);
    Event dequeue();
    int size();

    <E extends Event> SubscribableEvent<E> onNewEvent();

    void clear();
}
