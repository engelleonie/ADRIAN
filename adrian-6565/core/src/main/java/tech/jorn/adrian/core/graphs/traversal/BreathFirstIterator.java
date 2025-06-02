package tech.jorn.adrian.core.graphs.traversal;

import tech.jorn.adrian.core.graphs.base.IGraph;
import tech.jorn.adrian.core.graphs.base.INode;

import java.util.*;

public class BreathFirstIterator<N extends INode> implements IGraphSearch<N> {
    private final IGraph<N> graph;
    private final Set<String> visitedIds = new HashSet<>();

    public BreathFirstIterator(IGraph<N> graph) {
        this.graph = graph;
    }

    @Override
    public List<List<N>> findAllPathsTo(N start, N goal) {
        List<List<N>> paths = new ArrayList<>();
        Queue<List<N>> queue = new LinkedList<>();

        queue.add(new ArrayList<>(Arrays.asList(start)));

        while (!queue.isEmpty()) {
            List<N> currentPath = queue.poll();
            N current = currentPath.get(currentPath.size() - 1);

            if (current.equals(goal)) paths.add(currentPath);
            if (currentPath.size() > 5) continue;

            for (N neighbour : graph.getNeighbours(current)) {
                if (currentPath.contains((neighbour))) continue;
                List<N> newPath = new ArrayList<>(currentPath);
                newPath.add(neighbour);
                queue.add(newPath);
            }
        }

        return paths;
    }
}
