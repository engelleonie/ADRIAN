package tech.jorn.adrian.core.risks.validators;

import tech.jorn.adrian.core.properties.AbstractProperty;

@FunctionalInterface
public interface PropertyValidator<T> {
    boolean validate(T property);
}
