package tech.jorn.adrian.core.controllers;

import tech.jorn.adrian.core.agents.AgentState;
import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.events.EventManager;
import tech.jorn.adrian.core.observables.SubscribableValueEvent;

public abstract class AbstractEventController<E extends Event> extends AbstractController {

    public AbstractEventController(Class<E> eventType, EventManager eventManager, SubscribableValueEvent<AgentState> agentState) {
        super(eventManager, agentState);

        this.registerEvents(eventType);
    }

    protected void registerEvents(Class<E> eventType) {
        this.eventManager.registerEventHandler(eventType, this::processEvent);
    }

    protected abstract void processEvent(E event);
}
