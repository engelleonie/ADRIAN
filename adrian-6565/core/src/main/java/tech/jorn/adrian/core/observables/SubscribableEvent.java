package tech.jorn.adrian.core.observables;

import java.util.function.Consumer;

public class SubscribableEvent<T> extends Subscribable<T, Consumer<T>> {
    public SubscribableEvent(EventDispatcher<T> dispatcher) {
        super(dispatcher);
    }

    public Runnable subscribe(Runnable handler) {
        return this.dispatcher.subscribe(nothing -> handler.run());
    }
}