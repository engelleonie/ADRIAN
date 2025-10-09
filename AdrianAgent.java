package tech.jorn.adrian.agent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tech.jorn.adrian.agent.controllers.RiskController;
import tech.jorn.adrian.agent.events.AgentSleepingEvent;
import tech.jorn.adrian.agent.events.ChangeStateEvent;
import tech.jorn.adrian.core.GlobalQueue;
import tech.jorn.adrian.core.agents.AgentState;
import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.agents.IAgentConfiguration;
import tech.jorn.adrian.core.controllers.IController;
import tech.jorn.adrian.core.observables.SubscribableValueEvent;
import tech.jorn.adrian.core.observables.ValueDispatcher;
import tech.jorn.adrian.core.events.EventManager;


import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
//import java.util.concurrent.ThreadLocalRandom;

public class AdrianAgent implements IAgent {
    private Logger log;

    protected final List<IController> controllers;
    private final IAgentConfiguration configuration;
    private final ValueDispatcher<AgentState> agentState;

    private final EventManager eventManager;

    private long lastActiveTimestamp = System.currentTimeMillis();
    private static final long SLEEP_THRESHOLD_MS = 5000;

    private int searchCount = 0;
    private boolean unsuccessfulSearch = false;
    private final AtomicInteger activeEventCount = new AtomicInteger(0);
    private int failedSearches = 0;
    private int identifyAttempts = 0;
    private int repeatedBehaviorCount = 0;
    private static final int MAX_REPEATED_BEHAVIOR = 10;

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

    public void startReady() {
        // decoupling of ready and idle statechange? adding a timer?

        this.agentState.setCurrent(AgentState.Ready);
    }

    public void start() {

    }

    public void startIdle() {
        this.agentState.setCurrent(AgentState.Idle);
    }


    public void recordSearchResult(boolean success) {
        searchCount++;

        if (success) {
            searchCount = 0;
        }

        if (searchCount >= 2 && unsuccessfulSearch) {
            shutdownAgent();
        }
    }

    public void setUnsuccessfulSearch() {
        this.unsuccessfulSearch = true;
    }

    private void shutdownAgent() {
        agentState.setCurrent(AgentState.Shutdown);
        System.out.println("Agent wird heruntergefahren!");
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

    public void markActive() {
        lastActiveTimestamp = System.currentTimeMillis();
        if (getState() == AgentState.Idle) {
            setState(AgentState.Ready);
        }
    }

    public void checkSleep() {
        if (getState() != AgentState.Shutdown &&
                System.currentTimeMillis() - lastActiveTimestamp > SLEEP_THRESHOLD_MS) {
            setState(AgentState.Idle);
        }
    }

    @Override
    public String getID() {
        return this.configuration.getNodeID();
    }

    @Override
    public void setState(AgentState state) {
        agentState.setCurrent(state);
    }

    @Override
    public EventManager getEventManager() {
        return eventManager;
    }

    public void setControllers(List<IController> controllers) {
        this.controllers.clear();
        this.controllers.addAll(controllers);
    }

    public int getActiveEventCount() {
        return activeEventCount.get();
    }

    public void onStartProcessingEvent() {
        activeEventCount.incrementAndGet();
    }

    public void onFinishProcessingEvent() {
        activeEventCount.decrementAndGet();
    }

    public void addFailedSearch() {
        failedSearches++;
    }

    public void resetFailedSearches() {
        failedSearches = 0;
    }

    public int getFailedSearches() {
        return failedSearches;
    }


    public int getIdentifyAttempts() {
        return identifyAttempts;
    }

    public void incrementIdentifyAttempts() {
        identifyAttempts++;
    }
    public void decrementIdentifyAttempts() {
        identifyAttempts--;
    }

    public void incrementRepeatedBehavior() {
        repeatedBehaviorCount++;
    }

    public int getRepeatedBehaviorCount() {
        return repeatedBehaviorCount;
    }

    public void resetRepeatedBehavior() {
        repeatedBehaviorCount--;
    }

    public int getMaxRepeatedBehavior() {
        return MAX_REPEATED_BEHAVIOR;
    }
}
