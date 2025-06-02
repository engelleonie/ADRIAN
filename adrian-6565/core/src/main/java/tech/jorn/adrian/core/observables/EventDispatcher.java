package tech.jorn.adrian.core.observables;

import java.util.function.Consumer;

public class EventDispatcher<T> extends AbstractEventDispatcher<T, Consumer<T>> {

    public final SubscribableEvent<T> subscribable = new SubscribableEvent<>(this);

    public EventDispatcher() {
    }

    public void dispatch(T value) {
        this.notifySubscribers(value);
    }
}