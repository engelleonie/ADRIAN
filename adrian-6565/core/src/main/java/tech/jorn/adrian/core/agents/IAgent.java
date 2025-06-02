package tech.jorn.adrian.core.agents;

import tech.jorn.adrian.core.observables.SubscribableValueEvent;

public interface IAgent {

    void start();
    SubscribableValueEvent<AgentState> onStateChange();
    AgentState getState();

    IAgentConfiguration getConfiguration();
}
