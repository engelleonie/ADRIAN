package tech.jorn.adrian.agent;

import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.graphs.base.INode;

import java.util.HashMap;
import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;

public class NodeRegistry {
    private static final NodeRegistry instance = new NodeRegistry();
    private final Map<String, INode> nodeMap = new HashMap<>();
    private final Map<String, IAgent> agentMap = new HashMap<>();

    private NodeRegistry() {}

    public static NodeRegistry getInstance() {
        return instance;
    }

    public void registerNode(INode node) {
        nodeMap.put(node.getID(), node);
    }

    public INode getNodeById(String id) {
        return nodeMap.get(id);
    }

    public void registerAgent(IAgent agent) {
        agentMap.put(agent.getID(), agent);
    }

    public IAgent getAgentByNodeId(String nodeId) {
        return agentMap.get(nodeId);
    }

    public boolean hasAgent(String nodeId) {
        return agentMap.containsKey(nodeId);
    }
}

