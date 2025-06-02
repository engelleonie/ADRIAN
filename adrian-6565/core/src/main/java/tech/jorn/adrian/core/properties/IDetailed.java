package tech.jorn.adrian.core.properties;

import tech.jorn.adrian.core.observables.SubscribableEvent;

import java.util.Map;
import java.util.Optional;

public interface IDetailed<P extends AbstractProperty<?>> {
    Map<String, P> getProperties();
    <T> Optional<T> getProperty(String property);
    void setFromProperty(String property, AbstractProperty<?> value);
    <T> void setProperty(String property, T value);
    SubscribableEvent<P> onPropertyChange();
}
