package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.observables.FlagDispatcher;

public class FinishScenarioEvent extends Event {
    private final FlagDispatcher finished;

    public FinishScenarioEvent(FlagDispatcher finished, IAgent agent) {
        super(agent);
        this.finished = finished;
    }


    public void trigger() {
        finished.raise();
    }
}

