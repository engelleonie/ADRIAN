package tech.jorn.adrian.core.graphs.infrastructure;

import tech.jorn.adrian.core.graphs.base.AbstractGraph;
import tech.jorn.adrian.core.graphs.base.GraphLink;

import java.util.List;

public class Infrastructure extends AbstractGraph<InfrastructureEntry<?>, GraphLink<InfrastructureEntry<?>>> {

    public List<InfrastructureNode> listNodes() {
        return this.nodes.stream()
                .filter(n -> n instanceof InfrastructureNode)
                .map(n -> (InfrastructureNode) n)
                .toList();
    }

    public List<SoftwareAsset> listSoftwareAssets() {
        return this.nodes.stream()
                .filter(n -> n instanceof SoftwareAsset)
                .map(n -> (SoftwareAsset) n)
                .toList();
    }
}
