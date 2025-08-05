package tech.jorn.adrian.agent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tech.jorn.adrian.agent.controllers.RiskController;
import tech.jorn.adrian.agent.events.ChangeStateEvent;
import tech.jorn.adrian.core.agents.AgentState;
import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.agents.IAgentConfiguration;
import tech.jorn.adrian.core.controllers.IController;
import tech.jorn.adrian.core.observables.SubscribableValueEvent;
import tech.jorn.adrian.core.observables.ValueDispatcher;
import tech.jorn.adrian.core.events.EventManager;


import java.util.List;
//import java.util.concurrent.ThreadLocalRandom;

public class AdrianAgent implements IAgent {
    private Logger log;

    protected final List<IController> controllers;
    private final IAgentConfiguration configuration;
    private final ValueDispatcher<AgentState> agentState;

    private final EventManager eventManager;



    public AdrianAgent(List<IController> controllers, IAgentConfiguration configuration, ValueDispatcher<AgentState> agentState, EventManager eventManager) {
        this.controllers = controllers;
        this.configuration = configuration;
        this.agentState = agentState;
        this.eventManager = eventManager;

        this.log = LogManager.getLogger(String.format("[%s] %s", configuration.getNodeID(), "AdrianAgent"));

        this.agentState.subscribe(state -> {
            this.log.debug("Agent state changed to {}", state);
        });
    }

    public void start() {
        // decoupling of ready and idle statechange? adding a timer?

        this.agentState.setCurrent(AgentState.Ready);

        this.agentState.setCurrent(AgentState.Idle);
    }

    public void stop() {
        this.agentState.setCurrent(AgentState.Shutdown);

        //RiskController riskController = (RiskController) this.controllers.stream().filter(c -> c instanceof RiskController).findFirst().get();
        //riskController.stop();
    }

    @Override
    public SubscribableValueEvent<AgentState> onStateChange() {
        return this.agentState.subscribable;
    }

    @Override
    public AgentState getState() {
        return this.agentState.current();
    }

    public IAgentConfiguration getConfiguration() {
        return this.configuration;
    }
}
