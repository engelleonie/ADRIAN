package tech.jorn.adrian.core.graphs.infrastructure;

import tech.jorn.adrian.core.properties.SoftwareProperty;

public class SoftwareAsset extends InfrastructureEntry<SoftwareProperty<?>> {

    public SoftwareAsset(String id) {
        super(id);
    }

    @Override
    protected <T> SoftwareProperty<?> makeProperty(String property, T value) {
        return new SoftwareProperty<>(property, value);
    }
}