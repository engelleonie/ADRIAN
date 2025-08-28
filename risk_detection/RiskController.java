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

import tech.jorn.adrian.agent.events.FoundRiskEvent;
import tech.jorn.adrian.agent.events.IdentifyRiskEvent;
import tech.jorn.adrian.agent.events.InitiateAuctionEvent;
import tech.jorn.adrian.agent.events.SelectedRiskEvent;
import tech.jorn.adrian.core.EventNode;
import tech.jorn.adrian.core.GlobalQueue;
import tech.jorn.adrian.core.agents.AgentState;
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




    public RiskController(RiskDetection riskDetection, KnowledgeBase knowledgeBase, EventManager eventManager,
            IRiskSelector riskSelector, IAgentConfiguration configuration,
            SubscribableValueEvent<AgentState> agentState) {
        super(eventManager, agentState);

        this.log = LogManager.getLogger(String.format("[%s] %s", configuration.getNodeID(), "RiskController"));

        this.riskDetection = riskDetection;
        this.knowledgeBase = knowledgeBase;
        this.riskSelector = riskSelector;
        this.configuration = configuration;


        this.eventManager.registerEventHandler(IdentifyRiskEvent.class, this::debounced);
        eventManager.registerEventHandler(IdentifyRiskEvent.class, e -> {
            System.out.println("Agent " + configuration.getNodeID()
                    + " hat ein IdentifyRiskEvent ausgeführt");
        });
        this.eventManager.registerEventHandler(IdentifyRiskEvent.class, e -> {
            System.out.println("DEBUG: Agent " + configuration.getNodeID() + " registriert Handler für IdentifyRiskEvent");
        });
        eventManager.registerEventHandler(IdentifyRiskEvent.class, e -> {
            System.out.println("Agent " + configuration.getNodeID() +
                    " received IdentifyRiskEvent for " + e.getAgentID());
        });
        this.eventManager.registerEventHandler(IdentifyRiskEvent.class, e -> {
            System.out.println("Agent " + this.configuration.getNodeID() + " verarbeitet Event von Agent " + e.getAgentID());
        });



        this.eventManager.registerEventHandler(FoundRiskEvent.class, this::foundRiskEvent);
        this.eventManager.registerEventHandler(SelectedRiskEvent.class, this::selectedRiskEvent);

        System.out.println("DEBUG RiskController init for " + configuration.getNodeID());

        System.out.println("RiskController für " + configuration.getNodeID()
                + " verwendet EventManager " + eventManager.hashCode());


        //this.riskAssessmentTimer = new Timer(String.format("timer-%s", configuration.getNodeID()));
        //this.scheduleRiskAssessment();
    }



      protected void debounced(IdentifyRiskEvent event) {
          String agentID = event.getAgentID();
          String myID = this.configuration.getNodeID();
          System.out.println("AgentID im Event=" + event.getAgentID() + ", Controller=" + myID);

          if (!agentID.equals(myID)) {
              return;
          }

          GlobalQueue q = GlobalQueue.getInstance();

          if (scheduledRiskEvents.containsKey(agentID)) {
              q.remove(scheduledRiskEvents.get(agentID));
          }

          event.markDue();
          scheduledRiskEvents.put(agentID, event);
          q.offer(event, 100);

          // Event jetzt ausführen
          identifyRisk(event);
          scheduledRiskEvents.remove(agentID);

          // Nach Ausführung löschen
           //scheduledRiskEvents.remove(agentID);
          //scheduledRiskEvent = event;
          //GlobalQueue.getInstance().offer(event, scheduledTime);

        //this.riskAssessmentFuture = this.riskAssessmentScheduler.schedule(this::identifyRisk, 100, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    protected void identifyRisk(IdentifyRiskEvent event) {


        System.out.println("SimTime=" + GlobalQueue.getInstance().getSimulatedTime() +
                ", KnowledgeBase nodes=" + this.knowledgeBase.getNodes().size());
        System.out.println(101010);
        var attackGraph = this.riskDetection.createAttackGraph(this.knowledgeBase);
        log.info("Nodes in graph: {}", attackGraph.getNodes().size());
        log.info("Incoming edges: {}", attackGraph.getIncoming().size());

        log.info("AttackGraph nodes={}",
                attackGraph.getNodes().size());
        System.out.println(202020);
        var risks = this.riskDetection.identifyRisks(attackGraph, true);
        System.out.println("identifyRisk called at simTime=" + GlobalQueue.getInstance().getSimulatedTime());
        System.out.println("Graph currently has " + attackGraph.getNodes().size() + " nodes");


        if (this.lastRiskReport != null) {
            System.out.println(20);
            // Filter-out any risks that we previously processed
            risks.removeIf(r -> r.toString().equals(this.lastRiskReport.toString()));
        }

        /* if(risks.isEmpty()) {
            System.out.println(303030);
            GlobalQueue.getInstance().offer(new IdentifyRiskEvent(), 50);
            return;
        } */

        var risk = this.riskSelector.select(risks);
        System.out.println(30);
        this.log.debug("Found {} risks{}", risks.size(), risk
                .map(riskReport -> " and selected " + riskReport.toShortString())
                .orElse(""));
        System.out.println(40);

        risks.forEach(r -> this.eventManager.emit(new FoundRiskEvent(r)));
        System.out.println(50);
        risk.ifPresentOrElse(

                r -> this.eventManager.emit(new SelectedRiskEvent(r)),
                () -> this.log.warn("No risk was found with the given constraints"));
    }

    protected void foundRiskEvent(FoundRiskEvent event) {

        // needs RiskReport as parameter
        this.eventManager.emit(new InitiateAuctionEvent(event.getRiskReport()));
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
        return this.agentState.current().equals(AgentState.Idle);
    }

    public void stop() {
        //this.riskAssessmentTimer.cancel();
        //this.riskAssessmentScheduler.shutdownNow();
    }

    public static int getIdentifyRiskCallCount() {
        return identifyRiskCallCount;
    }
}
