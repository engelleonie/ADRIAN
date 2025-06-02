package tech.jorn.adrian.core.observables;

import java.util.function.Consumer;

public class SubscribableFlagEvent extends Subscribable<Void, Consumer<Void>> {
    public SubscribableFlagEvent(FlagDispatcher dispatcher) {
        super(dispatcher);
    }

    public Runnable subscribe(Runnable handler) {
        return this.dispatcher.subscribe(nothing -> handler.run());
    }
}
