package tech.jorn.adrian.core.events.queue;

import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.observables.EventDispatcher;
import tech.jorn.adrian.core.observables.SubscribableEvent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class InMemoryQueue implements IEventQueue {
    private final Queue<Event> queue;
    private final EventDispatcher<Event> eventDispatcher;

    public InMemoryQueue() {
        this.queue = new ConcurrentLinkedQueue<>();
        this.eventDispatcher = new EventDispatcher<>();
    }

    @Override
    public void enqueue(Event event) {
        this.queue.add(event);
        this.eventDispatcher.dispatch(event);
    }

    @Override
    public Event dequeue() {
        if (this.queue.size() == 0) return null;
        return this.queue.poll();
    }

    @Override
    public int size() {
        return this.queue.size();
    }

    @Override
    public <E extends Event> SubscribableEvent<E> onNewEvent() {
        return (SubscribableEvent<E>) this.eventDispatcher.subscribable;
    }

    @Override
    public void clear() {
        this.queue.clear();
    }
}
