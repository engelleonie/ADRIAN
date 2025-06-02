package tech.jorn.adrian.experiment;

import tech.jorn.adrian.agent.AgentConfiguration;
import tech.jorn.adrian.core.graphs.infrastructure.InfrastructureNode;
import tech.jorn.adrian.core.graphs.infrastructure.SoftwareAsset;

import java.util.List;

public class ExperimentalConfiguration extends AgentConfiguration {
    public ExperimentalConfiguration(InfrastructureNode parent, List<String> neighbours, List<SoftwareAsset> assets) {
        super(parent, neighbours, assets);
    }

    @Override
    public boolean canMigrate() {
        return false;
    }
}
