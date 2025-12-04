package tech.jorn.adrian.agent.controllers;

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
    private RiskReport lastRiskReport;
    private static int identifyRiskCallCount = 0;

    public RiskController(RiskDetection riskDetection, KnowledgeBase knowledgeBase, EventManager eventManager,
            IRiskSelector riskSelector, AdrianAgent agent) {
        super(eventManager, agent.onStateChange());

        this.riskDetection = riskDetection;
        this.knowledgeBase = knowledgeBase;
        this.riskSelector = riskSelector;
        this.agent = agent;
        this.configuration = agent.getConfiguration();
        this.log = LogManager.getLogger(String.format("[%s] %s", configuration.getNodeID(), "RiskController"));

        //registering handlers for when these Events are called
        this.eventManager.registerEventHandler(IdentifyRiskEvent.class, this::debounced);
        this.eventManager.registerEventHandler(FoundRiskEvent.class, this::foundRisk);
        this.eventManager.registerEventHandler(SelectedRiskEvent.class, this::selectedRiskEvent);
    }

    protected void debounced(IdentifyRiskEvent event) {
        String agentID = event.getAgentID();
        String myID = this.configuration.getNodeID();

        // in case agent was sent to the wrong riskController
        if (!agentID.equals(myID)) return;

        // testing if agent is capable of handling risk search
        if (!(agentState.current() == AgentState.Ready || agentState.current() == AgentState.Idle)) {
            log.debug("Agent {} not free (state={}), skipping IdentifyRiskEvent",
                    configuration.getNodeID(), agentState.current());
            return;
        }

        //starting identifyRisk
        identifyRisk(event);
        knowledgeBase.resetChangedFlag();
    }

    public void identifyRisk(IdentifyRiskEvent event) {
        log.debug("[{}] identifyRisk() called (state={})",
                configuration.getNodeID(), agent.getState());

        identifyRiskCallCount++;

        if (!canDoRiskAssessment()) {
            log.debug("Skipping IdentifyRiskEvent due to agent state: {}", this.agentState.current());
            return;
        }
        agent.setState(AgentState.Searching);

        var attackGraph = this.riskDetection.createAttackGraph(this.knowledgeBase);
        var risks = this.riskDetection.identifyRisks(attackGraph, true);

        log.info("[{}] Risks found: {}", configuration.getNodeID(),
                risks.stream().map(RiskReport::toShortString).collect(Collectors.joining(", ")));


        var selectedRisk = this.riskSelector.select(risks);
        log.info("[{}] Selected risk: {}", configuration.getNodeID(),
                selectedRisk.map(RiskReport::toShortString).orElse("none"));


        selectedRisk.ifPresentOrElse(risk -> {
            this.log.debug("Selected risk {}", risk.toShortString());

            //starting to deal with found risk
            this.lastRiskReport = risk;
            agent.setState(AgentState.Busy);
            this.eventManager.emit(new FoundRiskEvent(risk, event.getAgent()));
        }, () -> {
            this.log.warn("No risk was found");
            
            agent.setState(AgentState.Idle);

     });
        log.info("[{}] Agent state after identifyRisk(): {}", configuration.getNodeID(), agentState.current());

    }

    protected void foundRisk(FoundRiskEvent event) {
        log.info("[{}] foundRiskEvent received (risk={})", configuration.getNodeID(), event.getRiskReport().toShortString());
        this.eventManager.emit(new SelectedRiskEvent(event.getRiskReport(), event.getAgent()));

    }

    protected void selectedRiskEvent(SelectedRiskEvent event) {
        log.info("[{}] selectedRiskEvent received: {}", configuration.getNodeID(),
                event.getRiskReport().toShortString());

        // if (!this.canDoRiskAssessment()) return;
        log.info("Selected risk with probability {} and damage value {} (path: {})",
                event.getRiskReport().probability(),
                event.getRiskReport().damage(),
                event.getRiskReport().path()
                        .stream()
                        .map(INode::getID)
                        .collect(Collectors.joining(" -> ")));
        this.eventManager.emit(new InitiateAuctionEvent(event.getRiskReport(), event.getAgent()));
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


}
