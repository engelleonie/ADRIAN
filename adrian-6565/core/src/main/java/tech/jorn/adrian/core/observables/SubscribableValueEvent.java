package tech.jorn.adrian.core.observables;

import java.util.function.Consumer;

public class SubscribableValueEvent<T> extends Subscribable<T, Consumer<T>> {

    public SubscribableValueEvent(ValueDispatcher<T> dispatcher) {
        super(dispatcher);
    }

    public Runnable subscribe(Consumer<T> handler) {
        return this.subscribe(handler, false);
    }
    public Runnable subscribe(Consumer<T> handler, boolean dispatchImmediately) {
        return ((ValueDispatcher<T>)this.dispatcher)
                .subscribe(handler, dispatchImmediately);
    }

    public T current() {
        return ((ValueDispatcher<T>)this.dispatcher).current();
    }
}
