package tech.jorn.adrian.core.graphs.base;

import tech.jorn.adrian.core.graphs.traversal.BreathFirstIterator;
import tech.jorn.adrian.core.graphs.traversal.IGraphSearch;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public abstract class AbstractGraph<N extends INode, L extends GraphLink<N>> implements IGraph<N> {
    protected final Set<N> nodes = ConcurrentHashMap.newKeySet();
    protected final Map<N, List<L>> adjacent = new ConcurrentHashMap<>();

    @Override
    public void upsertNode(N node) {
        if (nodes.contains(node)) nodes.remove(node);
        nodes.add(node); // This might be a bit extra ash we are updating the keys of the map

        var links = adjacent.getOrDefault(node, new CopyOnWriteArrayList<>());
        if (adjacent.containsKey(node)) adjacent.remove(node);
        adjacent.put(node, links);
    }

    @Override
    public void removeNode(N node) {
        this.nodes.remove(node);
        this.adjacent.values().forEach(link -> link.removeIf(l -> node.equals(l.getNode())));
        this.adjacent.remove(node);
    }

    @Override
    public void removeNode(String id) {
        var node = this.findById(id);
        node.ifPresent(this::removeNode);
    }

    @Override
    public void addEdge(N from, N to) {
        var adj = this.adjacent.getOrDefault(from, new ArrayList<>());
        adj.add((L) new GraphLink<>(to));
        this.adjacent.put(from, adj);
    }

    @Override
    public void removeEdge(N from, N to) {
        var adj = this.adjacent.getOrDefault(from, new ArrayList<>());
        adj.removeIf(l -> l.getNode().equals(to));
        this.adjacent.put(from, adj);
    }

    public void removeEdgeByID(String from, String to) {
        var parent = this.findById(from);
        var target = this.findById(to);

        if (parent.isEmpty()) return;
        if (target.isEmpty()) return;

        var adj = this.adjacent.getOrDefault(parent.get(), new ArrayList<>());
        adj.removeIf(l -> l.getNode().equals(target.get()));
        this.adjacent.put(parent.get(), adj);
    }

    @Override
    public Optional<N> findById(String id) {
        return this.adjacent.keySet().stream()
                .filter(n -> n.getID().equals(id))
                .findFirst();
    }

    @Override
    public List<N> getNeighbours(N node) {
        return this.adjacent.getOrDefault(node, List.of()).stream()
                .map(GraphLink::getNode)
                .collect(Collectors.toList());
    }

    @Override
    public List<N> getNeighbours(String id) {
        var node = this.findById(id);
        return node
                .map(this::getNeighbours)
                .orElse(null);
    }

    public List<N> getParents(N node) {
        var parents = new ArrayList<N>();
        this.adjacent.forEach((key, children) -> {
            var found = children.stream().filter(link -> link.equals(node))
                    .findAny();
            if (found.isPresent()) parents.add(key);
        });
        return parents;
    }

    public List<N> getParents(String id) {
        var node = this.findById(id);
        if (node.isEmpty()) return List.of();
        return this.getParents(node.get());
    }

    public List<L> getLinks(N node) {
        return this.adjacent.get(node);
    }

    @Override
    public Set<N> getNodes() {
        return this.nodes;
    }

    public List<List<N>> findPathsTo(N from, N to) {
        return this.findPathsTo(from, to, new BreathFirstIterator<>(this));
    }

    public List<List<N>> findPathsTo(N from, N to, IGraphSearch<N> strategy) {
        return strategy.findAllPathsTo(from, to);
    }
}
