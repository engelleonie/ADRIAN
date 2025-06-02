package tech.jorn.adrian.experiment.instruments;

import org.apache.logging.log4j.LogManager;

import tech.jorn.adrian.core.agents.AgentState;
import tech.jorn.adrian.core.agents.IAgentConfiguration;
import tech.jorn.adrian.core.events.EventManager;
import tech.jorn.adrian.core.events.queue.IEventQueue;
import tech.jorn.adrian.core.observables.Subscribable;
import tech.jorn.adrian.core.observables.SubscribableValueEvent;

public class ExperimentalEventManager extends EventManager {

    public ExperimentalEventManager(IEventQueue eventQueue, IAgentConfiguration agentConfiguration, SubscribableValueEvent<AgentState> agentState) {
        super(eventQueue, agentState);
        //this.log = LogManager.getLogger("[" + agentConfiguration.getNodeID() + "] EventManager");
    }

    
}
