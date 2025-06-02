package tech.jorn.adrian.core.services;

import tech.jorn.adrian.core.graphs.infrastructure.Infrastructure;

import java.util.function.Consumer;

public interface InfrastructureEffector {
    void updateInfra(Consumer<Infrastructure> fn);
}
