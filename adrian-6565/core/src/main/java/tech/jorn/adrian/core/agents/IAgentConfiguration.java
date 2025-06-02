package tech.jorn.adrian.core.agents;

import tech.jorn.adrian.core.graphs.base.INode;
import tech.jorn.adrian.core.graphs.infrastructure.InfrastructureNode;
import tech.jorn.adrian.core.graphs.infrastructure.SoftwareAsset;

import java.util.List;

public interface IAgentConfiguration {
    InfrastructureNode getParentNode();

    List<String> getNeighbours();

    List<SoftwareAsset> getAssets();

    default String getNodeID() {
        return this.getParentNode().getID();
    }

    default int getAuctionTimeout() {
        return 4 * 1000;
    }

    default boolean canMigrate() {
        return (boolean) this.getParentNode()
                .getProperty("canMigrate")
                .orElse(false);
    }

}
