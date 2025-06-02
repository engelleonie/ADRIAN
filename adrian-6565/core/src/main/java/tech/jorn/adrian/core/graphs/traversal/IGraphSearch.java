package tech.jorn.adrian.core.graphs.traversal;

import tech.jorn.adrian.core.graphs.base.INode;

import java.util.List;

public interface IGraphSearch<N extends INode> {
    List<List<N>> findAllPathsTo(N start, N goal);
}