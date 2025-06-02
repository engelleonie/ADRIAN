package tech.jorn.adrian.core.properties;

public interface IProperty<T> {
    String getName();
    T getValue();
    Object setValue(T value);
}
