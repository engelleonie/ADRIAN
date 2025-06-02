package tech.jorn.adrian.core.services;

import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBase;
import tech.jorn.adrian.core.graphs.risks.AttackGraph;
import tech.jorn.adrian.core.risks.RiskReport;

import java.util.List;

public interface RiskDetection {
    AttackGraph createAttackGraph(KnowledgeBase knowledgeBase);

    List<RiskReport> identifyRisks(AttackGraph attackGraph, boolean isContained);
}
