package tech.jorn.adrian.core.risks.validators;

public abstract class VersionValidator implements PropertyValidator<String> {
    private final Runtime.Version version;

    public VersionValidator(String version) {
        this.version = Runtime.Version.parse(version);
    }

    public Runtime.Version getVersion() {
        return version;
    }
}

