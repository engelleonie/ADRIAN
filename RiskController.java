package tech.jorn.adrian.agent.controllers;

/*import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService; */
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tech.jorn.adrian.agent.AdrianAgent;
import tech.jorn.adrian.agent.events.*;
import tech.jorn.adrian.core.EventNode;
import tech.jorn.adrian.core.GlobalQueue;
import tech.jorn.adrian.core.agents.AgentState;
import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.agents.IAgentConfiguration;
import tech.jorn.adrian.core.controllers.AbstractController;
import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.events.EventManager;
import tech.jorn.adrian.core.graphs.base.INode;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBase;
import tech.jorn.adrian.core.observables.SubscribableValueEvent;
import tech.jorn.adrian.core.risks.RiskReport;
import tech.jorn.adrian.core.services.RiskDetection;
import tech.jorn.adrian.core.services.risks.IRiskSelector;

public class RiskController extends AbstractController {
    Logger log = LogManager.getLogger(RiskController.class);
    private final RiskDetection riskDetection;
    private final IRiskSelector riskSelector;
    private final KnowledgeBase knowledgeBase;
    private final IAgentConfiguration configuration;
    private final AdrianAgent agent;
    // private Timer riskAssessmentTimer;
    // private ScheduledExecutorService riskAssessmentScheduler = Executors.newSingleThreadScheduledExecutor();
    // private Future<?> riskAssessmentFuture;
    private RiskReport lastRiskReport;
    private static int identifyRiskCallCount = 0;
    private long lastIdentifyTimestamp = 0;
    private boolean identifyRunning = false;
    private boolean identifyPending = false;
    private static final long MIN_INTERVAL_MS = 200;
    private long nextRiskAssessmentTime = -1;
    private final Map<String, IdentifyRiskEvent> scheduledRiskEvents = new ConcurrentHashMap<>();

    private static final int maxFailedSearches = 3;
    private boolean failedSearch = false;
    private static final GlobalQueue globalQueue = GlobalQueue.getInstance();



    public RiskController(RiskDetection riskDetection, KnowledgeBase knowledgeBase, EventManager eventManager,
            IRiskSelector riskSelector, AdrianAgent agent) {
        super(eventManager, agent.onStateChange());

        this.riskDetection = riskDetection;
        this.knowledgeBase = knowledgeBase;
        this.riskSelector = riskSelector;
        this.agent = agent;
        this.configuration = agent.getConfiguration();
        this.log = LogManager.getLogger(String.format("[%s] %s", configuration.getNodeID(), "RiskController"));


        this.eventManager.registerEventHandler(IdentifyRiskEvent.class, this::debounced);
        this.eventManager.registerEventHandler(FoundRiskEvent.class, this::foundRiskEvent);
        this.eventManager.registerEventHandler(SelectedRiskEvent.class, this::selectedRiskEvent);

        System.out.println("RiskController for " + configuration.getNodeID()
                + " uses EventManager " + eventManager.hashCode());

        //this.riskAssessmentTimer = new Timer(String.format("timer-%s", configuration.getNodeID()));
        //this.scheduleRiskAssessment();
    }

    protected void debounced(IdentifyRiskEvent event) {
        String agentID = event.getAgentID();
        String myID = this.configuration.getNodeID();

        if (!agentID.equals(myID)) return;

        if (!(agentState.current() == AgentState.Ready || agentState.current() == AgentState.Idle)) {
            log.debug("Agent {} not free (state={}), skipping IdentifyRiskEvent",
                    configuration.getNodeID(), agentState.current());
            return;
        }

        try {
            boolean shouldIdentify = false;

            if (lastIdentifyTimestamp == 0) {
                shouldIdentify = true;
            }

            else if (knowledgeBase.hasChanged()) {
                shouldIdentify = true;
            }

            else if (agent.getFailedSearches() > 0) {
                shouldIdentify = true;
            }

            if (shouldIdentify) {
                identifyRisk(event);
                lastIdentifyTimestamp = System.currentTimeMillis();
                knowledgeBase.resetChangedFlag();
                agent.incrementIdentifyAttempts();
            }

            log.debug("lastIdentifyTimestamp={}, hasChanged={}, failedSearches={}, identifyAttempts={}",
                    lastIdentifyTimestamp, knowledgeBase.hasChanged(), agent.getFailedSearches(),
                    agent.getIdentifyAttempts());

        } finally {
            log.debug("finally lastIdentifyTimestamp={}, hasChanged={}, failedSearches={}, identifyAttempts={}",
                    lastIdentifyTimestamp, knowledgeBase.hasChanged(), agent.getFailedSearches(),
                    agent.getIdentifyAttempts());
        }
    }


    public void identifyRisk(IdentifyRiskEvent event) {
        log.debug("[{}] identifyRisk() called (failedSearches={}, state={})",
                configuration.getNodeID(), agent.getFailedSearches(), agent.getState());

        identifyRiskCallCount++;



        if (!canDoRiskAssessment()) {
            log.debug("Skipping IdentifyRiskEvent due to agent state: {}", this.agentState.current());
            return;
        }
        agent.setState(AgentState.Searching);

        var attackGraph = this.riskDetection.createAttackGraph(this.knowledgeBase);
        var risks = this.riskDetection.identifyRisks(attackGraph, true);

        boolean sameAsBefore = (this.lastRiskReport != null && risks.size() == 0)
                || (this.lastRiskReport != null && risks.stream()
                .anyMatch(r -> r.toString().equals(this.lastRiskReport.toString())));

        if (sameAsBefore) {
            agent.incrementRepeatedBehavior();
            log.debug("[{}] repeated behavior detected (count={})", configuration.getNodeID(), agent.getRepeatedBehaviorCount());
        } else {
            agent.resetRepeatedBehavior();
        }


        var selectedRisk = this.riskSelector.select(risks);

        selectedRisk.ifPresentOrElse(risk -> {
            this.log.debug("Selected risk {}", risk.toShortString());

            this.lastRiskReport = risk;
            agent.resetFailedSearches();
            agent.resetRepeatedBehavior();
            if (agent.getState() == AgentState.Sleeping) {
                eventManager.emit(new AgentWakeUpEvent(agent.getID()));
                log.info("Agent {} wakes up because risk was found", configuration.getNodeID());

            }
            agent.setState(AgentState.Busy);
            this.eventManager.emit(new FoundRiskEvent(risk));
        }, () -> {

            agent.addFailedSearch();
            this.log.warn("No risk was found ({} consecutive failures)", agent.getFailedSearches());

            this.eventManager.emit(new IdentifyRiskEvent(agent.getID()));
            agent.setState(AgentState.Idle);





            //scheduleSleepCheck();
            /* if (this.failedSearches >= maxFailedSearches) {
                boolean agentIdle = (agent.getState() == AgentState.Idle || agent.getState() == AgentState.Ready);
                int pending = GlobalQueue.getInstance().countPendingEventsFor(agent.getID());
                int active = agent.getActiveEventCount();
                log.info("[{}] Trying to sleep (failed={}, pendingEvents={})", configuration.getNodeID(), failedSearches, pending);

                if (pending == 0 && active == 0) {
                    this.eventManager.emit(new AgentSleepingEvent(agent.getID()));
                }

            } else {

                if (knowledgeBase.hasChanged()) {
                    knowledgeBase.resetChangedFlag();
                    log.debug("[{}] KB changed → triggering IdentifyRiskEvent", configuration.getNodeID());
                    agent.getEventManager().emit(new IdentifyRiskEvent(agent.getID()));

                } else {
                    log.debug("[{}] Not re-triggering IdentifyRiskEvent (no KB change and not exceeding retry limit)", configuration.getNodeID());
                }
            }*/
            // Ensure failedSearches bounded
            //failedSearches = Math.min(failedSearches, maxFailedSearches);

     });
    }


    protected void foundRiskEvent(FoundRiskEvent event) {
        this.eventManager.emit(new SelectedRiskEvent(event.getRiskReport()));

    }

    protected void selectedRiskEvent(SelectedRiskEvent event) {
        // if (!this.canDoRiskAssessment()) return;
        log.info("Selected risk with probability {} and damage value {} (path: {})",
                event.getRiskReport().probability(),
                event.getRiskReport().damage(),
                event.getRiskReport().path()
                        .stream()
                        .map(INode::getID)
                        .collect(Collectors.joining(" -> ")));
        this.eventManager.emit(new InitiateAuctionEvent(event.getRiskReport()));
        this.lastRiskReport = event.getRiskReport();
    }

    /* private TimerTask createScheduledRiskAssessmentTask() {
        var log = this.log;
        var eventManager = this.eventManager;
        return new TimerTask() {
            @Override
            public void run() {
                if (!canDoRiskAssessment()) return;

                log.warn("Searching for risks due to inactivity");
                eventManager.emit(new IdentifyRiskEvent());
            }
        };
    } */

    //in main schleife integrieren
    /* private void scheduleRiskAssessment() {
        // if (this.riskAssessmentTimer != null) {
        //     this.riskAssessmentTimer.cancel();
        //     this.riskAssessmentTimer = null;
        // }

        try {
            var task = createScheduledRiskAssessmentTask();

            var interval = 30 * 1000;
            this.riskAssessmentTimer.scheduleAtFixedRate(task, interval, interval);
        } catch (Exception e) {
            log.error("Error while scheduling risk assessment");
        }
    } */

    private boolean canDoRiskAssessment() {
        return (this.agentState.current().equals(AgentState.Idle) || this.agentState.current().equals(AgentState.Ready));
    }

    public void stop() {
        //this.riskAssessmentTimer.cancel();
        //this.riskAssessmentScheduler.shutdownNow();
    }

    public static int getIdentifyRiskCallCount() {
        return identifyRiskCallCount;
    }

    public void scheduleSleepCheck() {
        int pending = GlobalQueue.getInstance().countPendingEventsFor(this.agent.getID());
        int active = this.agent.getActiveEventCount();

        log.debug("[{}] Sleep check: pending={}, active={}, failedSearches={}",
                this.agent.getID(), pending, active, agent.getFailedSearches());

        if (pending == 0 && active == 0 && agent.getFailedSearches() >= maxFailedSearches) {
            log.info("[{}] All work done, agent going to sleep", this.agent.getID());
            eventManager.emit(new AgentSleepingEvent(this.agent.getID()));
        }
    }

}
