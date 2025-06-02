package tech.jorn.adrian.experiment.instruments;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.jorn.adrian.agent.AdrianAgent;
import tech.jorn.adrian.agent.services.BasicRiskDetection;
import tech.jorn.adrian.core.agents.IAgentConfiguration;
import tech.jorn.adrian.core.graphs.MermaidGraphRenderer;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBase;
import tech.jorn.adrian.core.graphs.risks.AttackGraph;
import tech.jorn.adrian.core.graphs.risks.AttackGraphEntry;
import tech.jorn.adrian.core.graphs.risks.AttackGraphLink;
import tech.jorn.adrian.core.risks.RiskEdge;
import tech.jorn.adrian.core.risks.RiskReport;
import tech.jorn.adrian.core.risks.RiskRule;
import tech.jorn.adrian.core.services.RiskDetection;
import tech.jorn.adrian.core.services.probability.IRiskProbabilityCalculator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class ExperimentalRiskDetection extends BasicRiskDetection {
    Logger log;

    private AttackGraph latestGraph = null;


    public ExperimentalRiskDetection(List<RiskRule> riskRules, IRiskProbabilityCalculator probabilityCalculator, IAgentConfiguration configuration) {
        super(riskRules, probabilityCalculator, configuration);
        this.log = LogManager.getLogger(String.format("[%s] %s", configuration.getNodeID(), RiskDetection.class.getSimpleName()));
    }

    @Override
    public AttackGraph createAttackGraph(KnowledgeBase knowledgeBase) {
        var attackGraph = super.createAttackGraph(knowledgeBase);
        this.latestGraph = attackGraph;
        return attackGraph;
    }

    @Override
    public List<RiskReport> identifyRisks(AttackGraph attackGraph, boolean isContained) {
        try {
            Thread.sleep( (int) (Math.random() * 5));
        } catch (Exception e) {
        }

        return super.identifyRisks(attackGraph, isContained);
    }

    @Override
    public Consumer<RiskEdge> createRiskDispatcher(AttackGraph attackGraph) {
        var original = super.createRiskDispatcher(attackGraph);
        return e -> {
            // TODO: Measure the risk edges!
            this.log.trace("Found a risk edge: \033[4m{}\033[0m \033[4m{}\033[0m \033[1m{}\033[0m \033[1m{}\033[0m", e.from().getID(), e.to().getID(), e.risk().type(), e.risk().factor());
            original.accept(e);
        };
    }

    public AttackGraph getLastAttackGraph() {
        return this.latestGraph;
    }
}
