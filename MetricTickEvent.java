package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.observables.EventDispatcher;
import tech.jorn.adrian.core.observables.FlagDispatcher;

public class MetricTickEvent extends Event {
    private final long simTime;
    private final EventDispatcher<Void> dispatcher;

    public MetricTickEvent(long simTime, EventDispatcher<Void> dispatcher, IAgent agent) {
        super(agent);
        this.simTime = simTime;
        this.dispatcher = dispatcher;
    }

    public long getSimTime() {
        return simTime;
    }

    public void trigger() {
        dispatcher.dispatch(null);
    }
}
