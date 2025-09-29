package tech.jorn.adrian.agent.controllers;

import tech.jorn.adrian.agent.AdrianAgent;
import tech.jorn.adrian.agent.events.AgentSleepingEvent;
import tech.jorn.adrian.agent.events.AgentWakeUpEvent;
import tech.jorn.adrian.agent.events.KnowledgeBaseChangedEvent;
import tech.jorn.adrian.core.agents.AgentState;
import tech.jorn.adrian.core.controllers.AbstractController;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SleepController extends AbstractController {

    //private final List<AdrianAgent> agents;
    //private final Runnable onAllSleeping;
    private final AtomicInteger sleepingAgents = new AtomicInteger(0);
    private final AdrianAgent agent;

    public SleepController(AdrianAgent agent) {
        super(agent.getEventManager(), agent.onStateChange());
        this.agent = agent;

        initEventHandlers();
    }

    private void initEventHandlers() {
        agent.getEventManager().registerEventHandler(AgentSleepingEvent.class, event -> {
            System.out.println("Agent " + agent.getID() + " sleeping.");
            agent.setState(AgentState.Sleeping);
        });


        agent.getEventManager().registerEventHandler(AgentWakeUpEvent.class, event -> {
            System.out.println("Agent " + agent.getID() + " wakes up.");
            agent.setState(AgentState.Ready);
        });

        agent.getEventManager().registerEventHandler(KnowledgeBaseChangedEvent.class, event -> {
            System.out.println("KnowledgeBase of Agent " + agent.getID() + " has changed.");
            if (agent.getState() == AgentState.Sleeping) {
                agent.getEventManager().emit(new AgentWakeUpEvent(agent.getID()));
            }
        });
    }

}
