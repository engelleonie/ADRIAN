package tech.jorn.adrian.core.graphs.base;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface IGraph<N extends INode> {
    void upsertNode(N node);
    void removeNode(N node);
    void removeNode(String id);
    void addEdge(N from, N to);

    void removeEdge(N from, N to);

    Optional<N> findById(String id);
    List<N> getNeighbours(N node);
    List<N> getNeighbours(String id);
    Set<N> getNodes();
}
