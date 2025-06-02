package tech.jorn.adrian.core.observables;

import java.util.function.Consumer;

public class ValueDispatcher<T> extends AbstractEventDispatcher<T, Consumer<T>> {

    public final SubscribableValueEvent<T> subscribable = new SubscribableValueEvent<T>(this);
    private T value;

    public ValueDispatcher(T value) {
        this.value = value;
    }

    public T current() {
        return value;
    }

    public void setCurrent(T value) {
        this.value = value;
        this.notifySubscribers(value);
    }

    public Runnable subscribe(Consumer<T> handler) {
        return this.subscribe(handler, false);
    }
    public Runnable subscribe(Consumer<T> handler, boolean dispatchImmediately) {
        var unsubscribe = super.subscribe(handler);
        if (dispatchImmediately) handler.accept(this.value);
        return unsubscribe;
    }
}