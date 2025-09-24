package tech.jorn.adrian.agent.controllers;

import org.apache.logging.log4j.LogManager;
import tech.jorn.adrian.agent.events.FinishScenarioEvent;
import tech.jorn.adrian.agent.events.MetricTickEvent;
import tech.jorn.adrian.core.agents.AgentState;
import tech.jorn.adrian.core.controllers.AbstractController;
import tech.jorn.adrian.core.events.EventManager;
import tech.jorn.adrian.core.observables.SubscribableValueEvent;



public class SystemController extends AbstractController {

    public SystemController(EventManager eventManager, SubscribableValueEvent<AgentState> agentState) {
        super(eventManager, agentState);


        eventManager.registerEventHandler(FinishScenarioEvent.class, e -> {
            System.out.println("Received FinishScenarioEvent");
            e.trigger();
        });

        eventManager.registerEventHandler(MetricTickEvent.class, e -> {
            System.out.println("MetricTick at simTime=" + e.getSimTime());
            e.trigger();
        });
    }
}
