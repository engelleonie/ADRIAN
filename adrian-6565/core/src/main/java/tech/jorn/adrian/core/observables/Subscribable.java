package tech.jorn.adrian.core.observables;

import java.util.function.Consumer;

public class Subscribable<TValue, THandler extends Consumer<TValue>> {
    protected final AbstractEventDispatcher<TValue, THandler> dispatcher;

    public Subscribable(AbstractEventDispatcher<TValue, THandler> dispatcher) {
        this.dispatcher = dispatcher;
    }

    public Runnable subscribe(THandler handler) {
        return this.dispatcher.subscribe(handler);
    }

    public void unsubscribe(THandler handler) {
        this.dispatcher.unsubscribe(handler);
    }
}