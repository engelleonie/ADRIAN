package tech.jorn.adrian.experiment.scenarios;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.graphs.infrastructure.Infrastructure;
import tech.jorn.adrian.core.graphs.infrastructure.InfrastructureEntry;
import tech.jorn.adrian.core.graphs.infrastructure.InfrastructureNode;
import tech.jorn.adrian.core.graphs.infrastructure.SoftwareAsset;
import tech.jorn.adrian.core.observables.EventDispatcher;
import tech.jorn.adrian.experiment.instruments.ExperimentalAgent;
import tech.jorn.adrian.experiment.messages.Envelope;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LargeScenario extends Scenario {
    private final Function<InfrastructureNode, IAgent> agentFactory;
    private final List<InfrastructureNode> agentNodes = new ArrayList<>();

    Logger log = LogManager.getLogger(LargeScenario.class);

    public LargeScenario(Infrastructure infrastructure, EventDispatcher<Envelope> messageDispatcher, Function<InfrastructureNode, IAgent> agentFactory) {
        super(infrastructure, messageDispatcher, 4 * 60 * 1000 /* Twenty Minutes */);
        this.agentFactory = agentFactory;
    }

    @Override
    public void onScheduleEvents(Queue<ExperimentalAgent> agents) {
        var root = this.infrastructure.findById("root")
                .orElseThrow(() -> new IllegalStateException("Root node not found in the infrastructure"));
        this.createSubnets((InfrastructureNode) root);

        var scheduler = Executors.newScheduledThreadPool(this.agentNodes.size());
        this.agentNodes.forEach(a -> {
            var agent = this.agentFactory.apply(a);
            scheduler.execute(() -> {
                agent.start();
                this.newAgent.dispatch((ExperimentalAgent) agent);
            });
        });

        this.after(1 * 60 * 1000, () -> {
            this.log.debug("Waiting for silence");
            this.waitForSilence(10 * 1000, this.finished::raise);
        });
    }

    private void createSubnets(InfrastructureNode root) {
        for (var i = 0; i < 10; i++) {
            var subnetId = "subnet-" + i;
            var node = createNode(subnetId);
            infrastructure.upsertNode(node);
            infrastructure.addEdge(root, node);
            infrastructure.addEdge(node, root);

            createLocalCluster(node);
        };
    }

    private void createLocalCluster(InfrastructureNode subnetNode) {
        var ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        var subnetId = subnetNode.getID().substring(12);

        for (var i = 0; i < 10; i++) {
            var nodeId = subnetId + ALPHABET.charAt(i);
            var node = createNode(nodeId);
            infrastructure.upsertNode(node);
            infrastructure.addEdge(subnetNode, node);
            infrastructure.addEdge(node, subnetNode);

            var asset = createAsset(node.getID(), node.getID());
            infrastructure.upsertNode(asset);
            infrastructure.addEdge(node, asset);
            infrastructure.addEdge(asset, node);
        }
    }

    private InfrastructureNode createNode(String id) {
        var node = new InfrastructureNode("node-" + id);
        node.setProperty("name", "Node " + id);
        node.setProperty("hasFirewall", false);
        node.setProperty("isPhysicallySecured", false);
        node.setProperty("hasAgent", true);
        node.setProperty("os-contiki-ng/contiki-ng-version", "3.2.1");
        node.setProperty("firmware-qualcomm/apq8096-version", "1.1");
        this.agentNodes.add(node);
        return node;
    }

    private SoftwareAsset createAsset(String id, String nodeId) {
        var asset = new SoftwareAsset("asset-" + id);
        asset.setProperty("name", "Asset " + id );
        asset.setProperty("isCritical", true);
        asset.setProperty("isEncrypted", false);
        asset.setProperty("sdk-amazon/amazon_web_services_iot_device_sdk_v2-version", "1.1");
        asset.setProperty("sdk-google/cloud_iot_device_sdk_for_embedded_c-version", "1.0.1");
        return asset;
    }
}
