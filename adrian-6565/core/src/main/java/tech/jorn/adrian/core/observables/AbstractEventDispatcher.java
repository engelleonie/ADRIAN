package tech.jorn.adrian.core.observables;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public abstract class AbstractEventDispatcher<TValue, THandler extends Consumer<TValue>> {

    public final Subscribable<TValue, THandler> subscribable = new Subscribable<>(this);

    protected Set<THandler> subscribers = ConcurrentHashMap.newKeySet();

    Runnable subscribe(THandler handler) {
        this.subscribers.add(handler);
        return () -> this.unsubscribe(handler);
    }

    void unsubscribe(THandler handler) {
        this.subscribers.remove(handler);
    }

    void clear() {
        this.subscribers.clear();;
    }

    protected void notifySubscribers(TValue value) {
        this.subscribers.forEach(h -> h.accept(value));
    }
}
