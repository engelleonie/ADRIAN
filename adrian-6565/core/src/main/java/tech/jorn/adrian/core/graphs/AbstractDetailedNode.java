package tech.jorn.adrian.core.graphs;

import tech.jorn.adrian.core.graphs.base.AbstractNode;
import tech.jorn.adrian.core.observables.EventDispatcher;
import tech.jorn.adrian.core.observables.SubscribableEvent;
import tech.jorn.adrian.core.properties.AbstractProperty;
import tech.jorn.adrian.core.properties.IDetailed;
import tech.jorn.adrian.core.properties.NodeProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractDetailedNode<P extends AbstractProperty<?>> extends AbstractNode implements IDetailed<P> {
    private final Map<String, P> properties;
    private final EventDispatcher<P> onPropertyChange = new EventDispatcher<>();

    public AbstractDetailedNode(String id) {
        super(id);
        this.properties = new HashMap<>();
    }

    public Map<String, P> getProperties() {
        return properties;
    }

    @Override
    public <T> Optional<T> getProperty(String property) {
        if (this.properties.containsKey(property)) {
            var p = this.properties.get(property);
            var value = (T) p.getValue();
            return Optional.of(value);
        }
        return Optional.empty();
    }

    @Override
    public void setFromProperty(String key, AbstractProperty<?> property) {
        this.setProperty(key, property.getValue());
    }
    @Override
    public <T> void setProperty(String property, T value) {
        var p = this.makeProperty(property, value);
        this.properties.put(property, p);
        this.onPropertyChange.dispatch(p);
    }

    protected abstract <T> P makeProperty(String property, T value);

    @Override
    public SubscribableEvent<P> onPropertyChange() {
        return this.onPropertyChange.subscribable;
    }
}
