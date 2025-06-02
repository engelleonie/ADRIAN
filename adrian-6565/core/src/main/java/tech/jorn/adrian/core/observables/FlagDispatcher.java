package tech.jorn.adrian.core.observables;

import java.util.function.Consumer;

public class FlagDispatcher extends AbstractEventDispatcher<Void, Consumer<Void>> {
    public final SubscribableFlagEvent subscribable = new SubscribableFlagEvent(this);

    private boolean value = false;

    public void raise() {
        if (this.value) return;
        this.value = true;
        this.notifySubscribers(null);
    }

    public void reset() {
        this.value = false;
    }

    public boolean isRaised() {
        return this.value;
    }

    public Runnable subscribe(Runnable handler) {
        Consumer<Void> wrapped = nothing -> handler.run();
        this.subscribers.add(wrapped);
        return () -> this.unsubscribe(wrapped);
    }
}
