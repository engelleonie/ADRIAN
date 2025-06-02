package tech.jorn.adrian.experiment;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tech.jorn.adrian.agent.events.*;
import tech.jorn.adrian.agent.services.BasicRiskDetection;
import tech.jorn.adrian.core.agents.AgentState;
import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.agents.IAgentConfiguration;
import tech.jorn.adrian.core.graphs.MermaidGraphRenderer;
import tech.jorn.adrian.core.graphs.base.VoidNode;
import tech.jorn.adrian.core.graphs.infrastructure.Infrastructure;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBase;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBaseNode;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBaseSoftwareAsset;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeOrigin;
import tech.jorn.adrian.core.graphs.risks.AttackGraph;
import tech.jorn.adrian.core.graphs.risks.AttackGraphEntry;
import tech.jorn.adrian.core.graphs.risks.AttackGraphLink;
import tech.jorn.adrian.core.risks.RiskReport;
import tech.jorn.adrian.core.services.probability.ProductRiskProbability;
import tech.jorn.adrian.experiment.instruments.ExperimentalAgent;
import tech.jorn.adrian.risks.RiskLoader;

public class MetricCollector {
    private List<ExperimentalAgent> agents = new ArrayList<>();
    private Map<IAgent, AtomicInteger> messageCount = new ConcurrentHashMap<>();
    private Map<IAgent, AtomicInteger> proposalCount = new ConcurrentHashMap<>();
    private Map<IAgent, AtomicInteger> riskCount = new ConcurrentHashMap<>();
    private Map<String, RiskReport> riskUnique = new ConcurrentHashMap<>();
    private Map<IAgent, AtomicLong> auctioning = new ConcurrentHashMap<>();
    private Map<IAgent, AtomicLong> migrating = new ConcurrentHashMap<>();
    private Map<IAgent, AgentState> previousState = new ConcurrentHashMap<>();
    private Map<IAgent, Date> startTimes = new ConcurrentHashMap<>();

    private static Map<String, List<Object>> csv = new ConcurrentHashMap<>();
    private int tick = 0;

    private Logger log = LogManager.getLogger(ExperimentRunner.class);

    private Infrastructure infrastructure;

    public MetricCollector(Infrastructure infrastructure) {
        this.infrastructure = infrastructure;

        csv.put("messages-total", new ArrayList<>());
        csv.put("proposals-total", new ArrayList<>());
        csv.put("riskCount-total", new ArrayList<>());
        csv.put("riskCount-global", new ArrayList<>());
        csv.put("riskUnique-total", new ArrayList<>());
        csv.put("riskUnique-global", new ArrayList<>());
        csv.put("riskDamage-global", new ArrayList<>());
        csv.put("auctioning-time-global", new ArrayList<>());
        csv.put("migrating-time-global", new ArrayList<>());
    }

    public void listenToAgent(ExperimentalAgent agent) {
        this.agents.add(agent);
        this.proposalCount.put(agent, new AtomicInteger(0));
        System.out.println("Registering agent: " + agent.getConfiguration().getNodeID());

        System.out.println(1);
        agent.getMessageBroker().registerMessageHandler(message -> {
            System.out.println("Received Message: " + message);
            var count = messageCount.computeIfAbsent(agent, k -> new AtomicInteger());
            count.incrementAndGet();
        });
        agent.getEventManager().registerEventHandler(SearchForProposalEvent.class, event -> {
            System.out.println("Received SearchProposalEvent: " + event);
            var count = proposalCount.computeIfAbsent(agent, k -> new AtomicInteger());
            count.incrementAndGet();
            System.out.println("Search count for " + agent.getConfiguration().getNodeID() );

        });
        agent.getEventManager().registerEventHandler(FoundProposalEvent.class, event -> {
            System.out.println("Received FoundProposalEvent: " + event);
            var count = proposalCount.computeIfAbsent(agent, k -> new AtomicInteger());
            count.incrementAndGet();
            System.out.println("Proposal count for " + agent.getConfiguration().getNodeID() );

        });
        agent.getEventManager().registerEventHandler(FoundRiskEvent.class, event -> {
            System.out.println("Received FoundRiskEvent: " + event);
            var count = riskCount.computeIfAbsent(agent, k -> new AtomicInteger());
            count.incrementAndGet();

            riskUnique.put(event.getRiskReport().toString(), event.getRiskReport());
        });
        agent.getEventManager().registerEventHandler(ShareKnowledgeEvent.class, event -> {
            event.getKnowledgeBase().getNodes().forEach(knowledgeNode -> {
                if (!knowledgeNode.getID().equals(event.getOrigin().getID()))
                    return;
                if (!knowledgeNode.getKnowledgeOrigin().equals(KnowledgeOrigin.DIRECT))
                    return;

                var infrastructureNode = infrastructure.findById(knowledgeNode.getID());
                if (infrastructureNode.isEmpty()) {
                    log.error("Could not find node {} in infrastructure", knowledgeNode.getID());
                    return;
                }

                var node = infrastructureNode.get();
                node.getProperties().forEach((key, property) -> {
                    var knowledgeProperty = knowledgeNode.getProperty(key);
                    if (knowledgeProperty.isEmpty()) {
                        log.error("Knowledge property {} for node {} does not exist (source: {})", key,
                                knowledgeNode.getID(), event.getOrigin().getID());
                        return;
                    }
                    if (!knowledgeProperty.get().equals(property.getValue())) {
                        log.error(
                                "Knowledge property {} for node {} does not match infrastructure property {} (source: {})",
                                knowledgeProperty, knowledgeNode.getID(), property.getValue(),
                                event.getOrigin().getID());
                    }
                });

            });
        });
        agent.onStateChange().subscribe(state -> {
            if (state.equals(AgentState.Idle)) {
                var elapsedTime = new Date().getTime() - startTimes.getOrDefault(agent, new Date()).getTime();
                if (previousState.get(agent) == null) {
                } else if (previousState.get(agent).equals(AgentState.Auctioning)) {
                    var time = auctioning.computeIfAbsent(agent, k -> new AtomicLong());
                    time.addAndGet(elapsedTime);
                } else if (previousState.get(agent).equals(AgentState.Migrating)) {
                    var time = migrating.computeIfAbsent(agent, k -> new AtomicLong());
                    time.addAndGet(elapsedTime);
                }
                startTimes.remove(agent);
            } else if (state.equals(AgentState.Auctioning))
                startTimes.put(agent, new Date());
            else if (state.equals(AgentState.Migrating))
                startTimes.put(agent, new Date());
            previousState.put(agent, state);
        });

        var id = agent.getConfiguration().getNodeID();

        var metrics = List.of("messages", "proposals", "riskCount", "riskDamage", "riskDamage-%", "auctioning-time", "migrating-time");
        metrics.forEach(metric -> {
            var list = new ArrayList<>();
            csv.put(metric + "-" + id, list);

            for (var i = 0; i < tick; i++)
                list.add(0);
        });
       /* csv.put("messages-" + id, new ArrayList<>());
        csv.put("proposals-" + id, new ArrayList<>());
        csv.put("riskCount-" + id, new ArrayList<>());
         csv.put("riskDamage-" + id, new ArrayList<>());
         csv.put("riskDamage-%-" + id, new ArrayList<>());
         csv.put("auctioning-time-" + id, new ArrayList<>());
         csv.put("migrating-time-" + id, new ArrayList<>());
         csv.put("riskUnique-" + id, new ArrayList<>()); */
        System.out.println(2);
    }

    public void updateInterval(Queue<ExperimentalAgent> agents) {
        System.out.println(3);

        csv.compute("messages-total", (k, list) -> {
            list.add(messageCount.values().stream().mapToInt(AtomicInteger::get).sum());
            return list;
        });
        csv.compute("proposals-total", (k, list) -> {
            list.add(proposalCount.values().stream().mapToInt(AtomicInteger::get).sum());
            return list;
        });
        csv.compute("riskCount-total", (k, list) -> {
            list.add(riskCount.values().stream().mapToInt(AtomicInteger::get).sum());
            return list;
        });
        csv.compute("riskUnique-total", (k, list) -> {
            list.add(riskUnique.size());
            return list;
        });
        csv.compute("auctioning-time-global", (k, list) -> {
            list.add(auctioning.values().stream().mapToInt(AtomicLong::intValue).sum());
            return list;
        });
        csv.compute("migrating-time-global", (k, list) -> {
            list.add(migrating.values().stream().mapToInt(AtomicLong::intValue).sum());
            return list;
        });

        try {
            var risks = calculateRisksInInfrastructure(infrastructure);

            var riskSet = new HashSet<String>();
            var riskDamageGlobal = risks.stream().mapToDouble(RiskReport::damage).sum();
            risks.forEach(risk -> riskSet.add(risk.toString()));

            csv.compute("riskCount-global", (k, list) -> {
                list.add(risks.stream().filter(r -> r.damage() > 25).count());
                return list;
            });
            csv.compute("riskUnique-global", (k, list) -> {
                list.add(riskSet.size());
                return list;
            });
            csv.compute("riskDamage-global", (k, list) -> {
                list.add(String.format("%.2f", riskDamageGlobal));
                return list;
            });

            agents.forEach(agent -> {
                var id = agent.getConfiguration().getNodeID();
                csv.compute("messages-" + id, (k, list) -> {
                    list.add(messageCount.getOrDefault(agent, new AtomicInteger(0)).get());
                    return list;
                });
                csv.compute("proposals-" + id, (k, list) -> {
                    list.add(proposalCount.getOrDefault(agent, new AtomicInteger(0)).get());
                    return list;
                });

                var attackGraph = agent.getRiskDetection().getLastAttackGraph();
                if (attackGraph == null)
                    attackGraph = agent.getRiskDetection().createAttackGraph(agent.getKnowledgeBase());

                var agentRisks = agent.getRiskDetection().identifyRisks(attackGraph, false);

                csv.compute("riskCount-" + id, (k, list) -> {
                    list.add(agentRisks.size());
                    return list;
                });
                var riskDamage = agentRisks.stream().mapToDouble(RiskReport::damage).sum();
                csv.compute("riskDamage-" + id, (k, list) -> {
                    list.add(String.format("%.2f", riskDamage));
                    return list;
                });
                csv.compute("riskDamage-%-" + id, (k, list) -> {
                    list.add(String.format("%.4f", riskDamage / riskDamageGlobal));
                    return list;
                });

                csv.compute("auctioning-time-" + id, (k, list) -> {
                    list.add(auctioning.getOrDefault(agent, new AtomicLong(0)).intValue());
                    return list;
                });
                csv.compute("migrating-time-" + id, (k, list) -> {
                    list.add(migrating.getOrDefault(agent, new AtomicLong(0)).intValue());
                    return list;
                });
            });
            tick++;
        } catch (Exception e) {
            this.log.error(e);
        }
        System.out.println(4);
    }

    private List<RiskReport> calculateRisksInInfrastructure(Infrastructure infrastructure) {
        System.out.println(5);
        var knowledgeBase = new KnowledgeBase();
        var voidNode = VoidNode.forKnowledge();
        knowledgeBase.upsertNode(voidNode);
        infrastructure.listNodes().forEach(node -> {
            var knowledge = KnowledgeBaseNode.fromNode(node);
            knowledgeBase.upsertNode(knowledge);
            knowledgeBase.addEdge(voidNode, knowledge);
        });
        infrastructure.listSoftwareAssets().forEach(asset -> {
            var knowledge = KnowledgeBaseSoftwareAsset.fromNode(asset);
            knowledgeBase.upsertNode(knowledge);
        });
        knowledgeBase.getNodes().forEach(node -> {
            var neighbours = infrastructure.getNeighbours(node.getID());
            if (neighbours == null) return;
            neighbours.forEach(neighbour -> {
                var knowledgeNeighbour = knowledgeBase.findById(neighbour.getID()).get();
                knowledgeBase.addEdge(node, knowledgeNeighbour);
            });
        });

        var riskDetection = new BasicRiskDetection(RiskLoader.listRisks(), new ProductRiskProbability(), null);
        var attackGraph = riskDetection.createAttackGraph(knowledgeBase);
        var risks = riskDetection.identifyRisks(attackGraph, false);

        renderAttackGraph(null, attackGraph, tick);
        System.out.println(6);
        return risks;

    }

    private void renderAttackGraph(IAgentConfiguration configuration, AttackGraph graph, int graphCount) {
        var filename = String.format("./graphs/attackGraph-%s-%d.mmd", configuration == null ? "global" : configuration.getNodeID(), graphCount);
        try {
            var writer = new FileWriter(filename);
            var graphRender = new MermaidGraphRenderer<AttackGraphEntry<?>, AttackGraphLink<AttackGraphEntry<?>>>();
            var mmdGraph = graphRender.render(graph);
            writer.write("%% " + graphCount * 5000 + "\n");
            writer.write(mmdGraph);
            writer.close();
        } catch (IOException e) {
            System.err.println("SOMETHING WENT WRONG OUTPUTTING GRAPH " + e.toString());
            throw new RuntimeException(e);
        }
    }

    public synchronized void writeToCSV(Queue<ExperimentalAgent> agents, long endTime, String config) throws IOException {
        System.out.println(7);
        var fileWriter = new FileWriter("./metrics_" + config + ".csv");
        var writer = new PrintWriter(fileWriter, true);

        writer.printf("timestamps;0;%s;%s\n", IntStream.range(0, (int) (endTime / 5000)).mapToObj(i -> String.valueOf((i + 1) * 5000)).collect(Collectors.joining(";")), endTime);
        writer.printf("messages-total;%s\n", csv.get("messages-total").stream()
                .map(Object::toString)
                .collect(Collectors.joining(";")));
        agents.forEach(agent -> {
            var id = agent.getConfiguration().getNodeID();
            writer.printf("messages-%s;%s\n", id, csv.get("messages-" + id).stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(";")));
        });

        writer.printf("proposals-total;%s\n", csv.get("proposals-total").stream()
                .map(Object::toString)
                .collect(Collectors.joining(";")));
        agents.forEach(agent -> {
            var id = agent.getConfiguration().getNodeID();
            writer.printf("proposals-%s;%s\n", id, csv.get("proposals-" + id).stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(";")));
        });

        writer.printf("riskCount-global;%s\n", csv.get("riskCount-global").stream()
                .map(Object::toString)
                .collect(Collectors.joining(";")));
        writer.printf("riskCount-total;%s\n", csv.get("riskCount-total").stream()
                .map(Object::toString)
                .collect(Collectors.joining(";")));
        agents.forEach(agent -> {
            var id = agent.getConfiguration().getNodeID();
            writer.printf("riskCount-%s;%s\n", id, csv.get("riskCount-" + id).stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(";")));
        });

        writer.printf("riskUnique-global;%s\n", csv.get("riskUnique-global").stream()
                .map(Object::toString)
                .collect(Collectors.joining(";")));
        writer.printf("riskUnique-total;%s\n", csv.get("riskUnique-total").stream()
                .map(Object::toString)
                .collect(Collectors.joining(";")));

        writer.printf("riskDamage-global;%s\n", csv.get("riskDamage-global").stream()
                .map(Object::toString)
                .collect(Collectors.joining(";")));
        agents.forEach(agent -> {
            var id = agent.getConfiguration().getNodeID();
            writer.printf("riskDamage-%s;%s\n", id, csv.get("riskDamage-" + id).stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(";")));
            writer.printf("riskDamage-%%-%s;%s\n", id, csv.get("riskDamage-%-" + id).stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(";")));
        });

        writer.printf("auctioning-time-global;%s\n", csv.get("auctioning-time-global").stream()
                .map(Object::toString)
                .collect(Collectors.joining(";")));
        writer.printf("migrating-time-global;%s\n", csv.get("migrating-time-global").stream()
                .map(Object::toString)
                .collect(Collectors.joining(";")));

        agents.forEach(agent -> {
            var id = agent.getConfiguration().getNodeID();
            writer.printf("auctioning-time-%s;%s\n", id, csv.get("auctioning-time-" + id).stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(";")));
            writer.printf("migrating-time-%s;%s\n", id, csv.get("migrating-time-" + id).stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(";")));
        });

        writer.close();
        System.out.println(8);
        log.info("Wrote metrics to ./metrics.csv");
    }
}
