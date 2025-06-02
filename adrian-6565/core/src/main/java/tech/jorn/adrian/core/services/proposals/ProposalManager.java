package tech.jorn.adrian.core.services.proposals;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.jorn.adrian.core.agents.AgentState;
import tech.jorn.adrian.core.agents.IAgentConfiguration;
import tech.jorn.adrian.core.auction.Auction;
import tech.jorn.adrian.core.auction.AuctionProposal;
import tech.jorn.adrian.core.graphs.AbstractDetailedNode;
import tech.jorn.adrian.core.graphs.MermaidGraphRenderer;
import tech.jorn.adrian.core.graphs.base.INode;
import tech.jorn.adrian.core.graphs.infrastructure.SoftwareAsset;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBase;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBaseNode;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBaseSoftwareAsset;
import tech.jorn.adrian.core.graphs.risks.AttackGraphEntry;
import tech.jorn.adrian.core.graphs.risks.AttackGraphLink;
import tech.jorn.adrian.core.mutations.AttributeChange;
import tech.jorn.adrian.core.mutations.Migration;
import tech.jorn.adrian.core.mutations.Mutation;
import tech.jorn.adrian.core.mutations.SoftwareAttributeChange;
import tech.jorn.adrian.core.observables.ValueDispatcher;
import tech.jorn.adrian.core.properties.AbstractProperty;
import tech.jorn.adrian.core.properties.SoftwareProperty;
import tech.jorn.adrian.core.risks.Risk;
import tech.jorn.adrian.core.risks.RiskReport;
import tech.jorn.adrian.core.services.InfrastructureEffector;
import tech.jorn.adrian.core.services.RiskDetection;
import tech.jorn.adrian.core.risks.PropertyBasedRule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ProposalManager {
    Logger log;

    private final KnowledgeBase knowledgeBase;
    private final RiskDetection riskDetection;
    private final IProposalSelector proposalSelector;
    private final IAgentConfiguration configuration;
    private final ValueDispatcher<AgentState> agentState;
    private final InfrastructureEffector infrastructureEffector;

    public ProposalManager(KnowledgeBase knowledgeBase, RiskDetection riskDetection, IProposalSelector proposalSelector,
                           IAgentConfiguration configuration, ValueDispatcher<AgentState> agentState,
                           InfrastructureEffector infrastructureEffector) {
        this.knowledgeBase = knowledgeBase;
        this.riskDetection = riskDetection;
        this.proposalSelector = proposalSelector;
        this.configuration = configuration;
        this.agentState = agentState;
        this.infrastructureEffector = infrastructureEffector;

        this.log = LogManager.getLogger(String.format("[%s] %s", this.configuration.getNodeID(), ProposalManager.class.getSimpleName()));
    }

    public List<AuctionProposal> findProposals(Auction auction) {
        var riskReport = auction.getRiskReport();
        this.knowledgeBase.mergeAttackGraph(riskReport);

        // Calculate all risks before we apply any mutations
        var risksBefore = this.riskDetection.identifyRisks(
                this.riskDetection.createAttackGraph(knowledgeBase),
                false
        );

        // Figure out all possible mutations
        var mutations = this.generateMutations(knowledgeBase, riskReport);
        var proposals = new ArrayList<AuctionProposal>();
        this.log.debug("Found {} mutations", mutations.size());
        mutations.forEach(mutation -> {
            KnowledgeBase clonedKnowledgeBase = knowledgeBase.clone();
            var proposal = this.evaluateMutation(mutation, auction, clonedKnowledgeBase);
            if (proposal == null)
                return;

            var risksAfter = this.riskDetection.identifyRisks(
                    this.riskDetection.createAttackGraph(clonedKnowledgeBase),
                    false
            );

            var riskCountDelta = risksAfter.size() - risksBefore.size();
            var riskDamageDelta = risksAfter.stream().mapToDouble(RiskReport::damage).sum() - risksBefore.stream().mapToDouble(RiskReport::damage).sum();

            if (riskCountDelta > 5) {
                this.log.warn("Proposal skipped as it would introduce too many new risks (+{})", riskCountDelta);
                return;
            }
            if (riskDamageDelta > 10) {
                this.log.warn("Proposal skipped as it would introduce too much new damage (+{})", riskDamageDelta);
                return;
            }

            proposals.add(proposal);
            this.log.debug("New probability {} compared to old probability {} for {}",
                    proposal.updatedReport().probability(),
                    riskReport.probability(), mutation.toString());
            this.log.debug("New damage {} compared to old damage {}",
                    proposal.updatedReport().damage(),
                    riskReport.damage());
        });

        return proposals;
    }

    private AuctionProposal evaluateMutation(Mutation<?> mutation, Auction auction, KnowledgeBase clonedKnowledgeBase) {
        this.log.debug("Evaluating mutation {}", mutation.toString());

        if (mutation instanceof Migration<?, ?>)
            return this.evaluateMigration((Migration<?, ?>) mutation, auction, clonedKnowledgeBase);
        else if (mutation instanceof AttributeChange<?, ?> || mutation instanceof SoftwareAttributeChange<?, ?>)
            return this.evaluateAttributeChange((Mutation<AbstractDetailedNode<?>>) mutation, auction,
                    clonedKnowledgeBase);
        return null;
    }

    private AuctionProposal evaluateMigration(Migration<?, ?> migration, Auction auction, KnowledgeBase knowledgeBase) {
        var riskReport = auction.getRiskReport();
        // First we need to perform the migration on the knowledgebase
        var software = riskReport.path().get(riskReport.path().size() - 1);
        var knowledgeSoftware = knowledgeBase.findById(software.getID());
        var host = knowledgeBase.findById(riskReport.path().get(riskReport.path().size() - 2).getID());
        var target = knowledgeBase.findById(migration.getNode().getID());

//        MermaidGraphRenderer.forAttackGraph().toFile("./graphs/mutation-before.mmd", riskReport.graph(), riskReport.toString());

        // Sever connection between host and software
        knowledgeBase.removeEdge(host.get(), knowledgeSoftware.get());
        knowledgeBase.removeEdge(knowledgeSoftware.get(), host.get());

        // Add new connections between target and software
        knowledgeBase.addEdge(target.get(), knowledgeSoftware.get());
        knowledgeBase.addEdge(knowledgeSoftware.get(), target.get());

        // Calculate the new attack graph
        var attackGraph = this.riskDetection.createAttackGraph(knowledgeBase);

        // Calculate the new critical path
        List<AttackGraphEntry<?>> attackGraphPath = new ArrayList();
        var targetNode = riskReport.path().stream().filter(n -> target.get().getID().equals(n.getID())).findFirst();
        if (targetNode.isEmpty()) {
            log.warn("Target was missing from critical path because of a suggested migration. Inserting after auction host");
            var hostNode = riskReport.path().stream().filter(n -> n.getID() == auction.getHost().getID()).findFirst();
            if (hostNode.isEmpty()) {
                log.error("Cannot find hosting node, canceling proposal");
                return null;
            }
            var sliceStart = riskReport.path().indexOf(hostNode.get());
            for (var i = 0; i <= sliceStart; i++) {
                var n = riskReport.path().get(i);
                var attackNode = attackGraph.findById(n.getID());
                attackNode.ifPresent(attackGraphPath::add);
            }
            var self = attackGraph.findById(this.configuration.getNodeID()).get();
            attackGraphPath.add(self);

            var _software = attackGraph.findById(riskReport.path().get(riskReport.path().size() - 1).getID()).get();
            log.debug("New critical path: {} -> {}",
                    attackGraphPath.stream().map(INode::getID).collect(Collectors.joining(" -> ")),
                    _software.getID());
        } else {
            var sliceStart = riskReport.path().indexOf(targetNode.get());
            for (var i = 0; i <= sliceStart; i++) {
                var n = riskReport.path().get(i);
                var attackNode = attackGraph.findById(n.getID());
                attackNode.ifPresent(attackGraphPath::add);
            }
        }
        var attackSoftware = attackGraph.findById(software.getID());
        attackSoftware.ifPresent(attackGraphPath::add);
        var updatedReport = RiskReport.fromCriticalPath(attackGraph, attackGraphPath);
//        MermaidGraphRenderer.forAttackGraph().toFile("./mutation-after.mmd", attackGraph, updatedReport.toString());
        log.debug("Old path: {}", riskReport.toString());
        log.debug("New path2: {}", updatedReport.toString());
//        MermaidGraphRenderer.forAttackGraph().toFile("./graphs/" + auction.getId() + "-before.mmd", riskReport.graph(), riskReport.toString());
//        MermaidGraphRenderer.forAttackGraph().toFile("./graphs/" + auction.getId() + "-after.mmd", updatedReport.graph(), updatedReport.toString());

        return new AuctionProposal(
                this.configuration.getParentNode(),
                auction,
                migration,
                updatedReport
        );
    }

    private AuctionProposal evaluateAttributeChange(Mutation<AbstractDetailedNode<?>> attributeChange,
                                                    Auction auction, KnowledgeBase knowledgeBase) {
        var riskReport = auction.getRiskReport();
        // Apply the attribute change
        var nodeToMutate = knowledgeBase.findById(attributeChange.getNode().getID()).get();
        attributeChange.apply(nodeToMutate);
        knowledgeBase.upsertNode(nodeToMutate);

        var attackGraph = this.riskDetection.createAttackGraph(knowledgeBase);

//        riskReport.graph().getNodes().forEach(node -> {
//            var riskEdges = riskReport.graph().getNeighboursWithRisks(node);
//            var existingEdges = attackGraph.getNeighboursWithRisks(node).stream()
//                    .map(AttackGraphLink::getRisk).toList();
//
//            riskEdges.forEach(edge -> {
//                var hasBeenMutated = attributeChange.getRiskRule() == null
//                        && edge.getRisk().rule().getClass().equals(attributeChange.getRiskRule().getClass());
//                if (hasBeenMutated) {
//                    this.log.debug("Risk {} was just updated so we will not add it again", edge.getRisk().type());
//                    return;
//                }
//
//                var exists = existingEdges.stream().filter(link -> link.type().equals(edge.getRisk().type())).findAny();
//                if (exists.isEmpty()) {
//                    attackGraph.addEdge(node, edge.getNode(), edge.getRisk());
//                }
//            });
//        });
        List<AttackGraphEntry<?>> attackGraphPath = new ArrayList();

        riskReport.path().forEach(node -> {
            var attackNode = attackGraph.findById(node.getID());
            attackNode.ifPresent(attackGraphPath::add);
            if (attackNode.isEmpty())
                this.log.warn("Node not found in attack graph {}", node.getID());
        });
        var updatedReport = RiskReport.fromCriticalPath(attackGraph, attackGraphPath);

        log.debug("Old path: {}", riskReport.toString());
        log.debug("New path1: {}", updatedReport.toString());

        try {
            var path = Path.of("./graphs/" + auction.getId() + "/" + this.configuration.getNodeID() + "/" + attributeChange.getRiskRule().getClass().getSimpleName());
//            Files.createDirectories(path.getParent());
            Files.createDirectories(path);
            MermaidGraphRenderer.forAttackGraph().toFile(path.resolve("before.mmd").toString(), riskReport.graph(), attributeChange + "\n%% " + riskReport);
            MermaidGraphRenderer.forAttackGraph().toFile(path.resolve("after.mmd").toString(), updatedReport.graph(), attributeChange + "\n%% " + updatedReport);
        } catch (Exception e) {
            log.error(e);
        }

        return new AuctionProposal(
                this.configuration.getParentNode(),
                auction,
                attributeChange,
                updatedReport);
    }

    public Optional<AuctionProposal> selectProposal(List<AuctionProposal> proposals, Auction auction) {
        return this.proposalSelector.select(proposals, auction.getRiskReport().damage());
    }

    public <N extends AbstractDetailedNode<P>, P extends AbstractProperty<?>> List<Mutation<N>> generateMutations(
            KnowledgeBase knowledgeBase, RiskReport riskReport) {

        List<Mutation<N>> mutations = new java.util.ArrayList<>(List.of());
        var node = (N) knowledgeBase.findById(this.configuration.getNodeID()).get();

        var softwareAsset = (N) riskReport.path().get(riskReport.path().size() - 1);
        var isHosting = riskReport.path().get(riskReport.path().size() - 2).getID().equals(node.getID());
        var isOnCriticalPath = riskReport.path().stream().anyMatch(n -> n.getID().equals(this.configuration.getNodeID()));

        var mutators = this.getMutators(riskReport, node);
        mutators.forEach(mutator -> {
            if (mutator == null)
                return;
            this.log.debug("Found mutator {}, applicable? {}", mutator.toString(),
                    isHosting
                            ? mutator.isApplicable(softwareAsset)
                            : mutator.isApplicable(node));
            if (mutator instanceof SoftwareAttributeChange && isHosting && mutator.isApplicable(softwareAsset)) {
                mutations.add(mutator);
            } else if (mutator instanceof AttributeChange && mutator.isApplicable(node) && isOnCriticalPath) {
                mutations.add(mutator);
            } else if (mutator instanceof Migration && mutator.isApplicable(softwareAsset)) {
                mutations.add(mutator);
            }
        });
        return mutations;
    }

    private <N extends AbstractDetailedNode<P>, P extends AbstractProperty<?>> List<Mutation<N>> getMutators(
            RiskReport riskReport, N node) {
        var mutations = new ArrayList<Mutation<N>>();

        var attackGraph = riskReport.graph();
        var criticalAsset = (AbstractDetailedNode<SoftwareProperty<?>>) riskReport.path()
                .get(riskReport.path().size() - 1);
        var n = attackGraph.findById(node.getID());
        if (n.isPresent()) {
            var forwardRisks = riskReport.graph().getNeighboursWithRisks(n.get())
                    .stream().map(AttackGraphLink::getRisk).toList();
            var backwardRisks = riskReport.graph().getParentsWithRisks(n.get())
                    .stream().map(AttackGraphLink::getRisk).toList();
            var risks = new ArrayList<Risk>();
            risks.addAll(forwardRisks);
            risks.addAll(backwardRisks);

            risks.forEach(risk -> {
                if (risk.rule() instanceof PropertyBasedRule rule) {
                    var adaptation = rule.getAdaptation(node);
                    adaptation.ifPresent(mutations::add);
                    this.log.debug("Found adaptation: {} {}", adaptation, node);
                }
            });

            this.configuration.getAssets().forEach(asset -> {
                var s = attackGraph.findById(asset.getID());
                if (s.isEmpty())
                    return;

                risks.forEach(risk -> {
                    // if (!(risk.rule() instanceof CveRule<?>))
                    // return;
                    var adaptation = risk.rule().getAdaptation((N) s.get());
                    adaptation.ifPresent(mutations::add);
                });
            });
        }

        if (node instanceof KnowledgeBaseNode) {
            if (this.configuration.canMigrate() && riskReport.path().size() > 2) {
                var host = (N) riskReport.path().get(riskReport.path().size() - 2);
                var alreadyHosting = host.getID().equals(node.getID());
                if (!alreadyHosting)
                    mutations.add(new Migration<>(criticalAsset, host, node, 1000, 10000, null));
            }
        }
        this.log.debug("Suggesting {} mutations", mutations.size());
        return mutations;
    }

    public void applyProposal(AuctionProposal proposal) {
        if (!this.agentState.current().equals(AgentState.Idle)) {
            this.log.error("Can only apply proposals while idle");
            return;
        }
        this.agentState.setCurrent(AgentState.Migrating);

        var node = this.configuration.getParentNode();
        var mutation = proposal.mutation();

        try {
            Thread.sleep(Math.round(mutation.getTime()));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (mutation instanceof Migration<?, ?> migration) {
            if (migration.getFrom().getID().equals(node.getID())) {

                this.log.info("Migrating software {} : Removing from {}", migration.getAsset().getID(), node.getID());
                // Remove the software asset
                this.configuration.getAssets().removeIf(asset -> asset.getID().equals(migration.getAsset().getID()));
                infrastructureEffector.updateInfra(infra -> {
                    var from = infra.findById(node.getID()).get();
                    var asset = infra.findById(migration.getAsset().getID()).get();
                    infra.removeEdge(from, asset);
                    infra.removeEdge(asset, from);
                });
            } else {
                this.log.info("Migrating software {}: Adding to {}", migration.getAsset().getID(), node.getID());
                var asset = new SoftwareAsset(migration.getAsset().getID());
                migration.getAsset().getProperties().forEach(asset::setFromProperty);
                this.configuration.getAssets().add(asset);

                infrastructureEffector.updateInfra(infra -> {
                    var target = infra.findById(migration.getNode().getID()).get();
                    var _asset = infra.findById(migration.getAsset().getID()).get();
                    infra.addEdge(target, _asset);
                    infra.addEdge(_asset, target);
                });

                var knowledgeAsset = KnowledgeBaseSoftwareAsset.fromNode(asset);
                this.knowledgeBase.upsertNode(knowledgeAsset);
            }
            var knowledgeFrom = this.knowledgeBase.findById(migration.getFrom().getID());
            var knowledgeTarget = this.knowledgeBase.findById(migration.getNode().getID());
            var knowledgeAsset = this.knowledgeBase.findById(migration.getAsset().getID());

            this.knowledgeBase.removeEdge(knowledgeFrom.get(), knowledgeAsset.get());
            this.knowledgeBase.removeEdge(knowledgeAsset.get(), knowledgeFrom.get());
            this.knowledgeBase.addEdge(knowledgeTarget.get(), knowledgeAsset.get());
            this.knowledgeBase.addEdge(knowledgeAsset.get(), knowledgeTarget.get());
        } else {
            infrastructureEffector.updateInfra(infra -> {
                if (mutation instanceof AttributeChange<?, ?> attributeChange) {
                    var _node = infra.findById(this.configuration.getNodeID()).get();
                    mutation.apply(_node);
                    infra.upsertNode(_node);
                } else if (mutation instanceof SoftwareAttributeChange<?, ?> softwareAttributeChange) {
                    var _node = infra.findById(softwareAttributeChange.getNode().getID()).get();
                    mutation.apply(_node);
                    infra.upsertNode(_node);
                }
            });
            // proposal.mutation().apply(node);
            // this.knowledgeBase.upsertNode(KnowledgeBaseNode.fromNode(node)
            // .setKnowledgeOrigin(KnowledgeOrigin.DIRECT));
        }

        this.log.debug("Done applying proposal");
        if (mutation instanceof Migration<?, ?> migration)
            this.log.debug("Software {} is now on {}", migration.getAsset().getID(), migration.getNode().getID());
        else if (mutation instanceof AttributeChange<?, ?> attributeChange) {
            this.configuration.getParentNode().getProperty(attributeChange.getNewValue().getName())
                    .ifPresent(property -> {
                        this.log.debug("Property {} is now {}", attributeChange.getNewValue().getName(), property);
                    });
        }
        this.agentState.setCurrent(AgentState.Idle);
    }
}

@FunctionalInterface
interface IMutationFunction<N extends AbstractDetailedNode<?>> {
    Mutation<N> mutate(N node);
}
