package tech.jorn.adrian.core.controllers;

import tech.jorn.adrian.core.agents.AgentState;
import tech.jorn.adrian.core.events.EventManager;
import tech.jorn.adrian.core.observables.SubscribableValueEvent;

public abstract class AbstractController implements IController {
    protected final EventManager eventManager;
    protected final SubscribableValueEvent<AgentState> agentState;

    public AbstractController(EventManager eventManager, SubscribableValueEvent<AgentState> agentState) {
        this.eventManager = eventManager;
        this.agentState = agentState;
    }
}
