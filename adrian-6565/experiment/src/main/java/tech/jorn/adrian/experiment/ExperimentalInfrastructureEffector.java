package tech.jorn.adrian.experiment;

import tech.jorn.adrian.core.graphs.MermaidGraphRenderer;
import tech.jorn.adrian.core.graphs.base.GraphLink;
import tech.jorn.adrian.core.graphs.infrastructure.Infrastructure;
import tech.jorn.adrian.core.graphs.infrastructure.InfrastructureEntry;
import tech.jorn.adrian.core.services.InfrastructureEffector;

import java.util.function.Consumer;

public class ExperimentalInfrastructureEffector implements InfrastructureEffector {
    private final Infrastructure infrastructure;

    public ExperimentalInfrastructureEffector(Infrastructure infrastructure) {
        this.infrastructure = infrastructure;
    }
    @Override
    public void updateInfra(Consumer<Infrastructure> fn) {
        fn.accept(this.infrastructure);
    }
}
