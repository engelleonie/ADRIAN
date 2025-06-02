package tech.jorn.adrian.core.risks.validators;

public class AnyVersion implements PropertyValidator<String> {
    public AnyVersion() {

    }

    @Override
    public boolean validate(String property) {
        return !property.isEmpty();
    }
}

