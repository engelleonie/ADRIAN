package tech.jorn.adrian.core.risks.validators;

public class BeforeVersion extends VersionValidator {
    public BeforeVersion(String version) {
        super(version);
    }

    public boolean validate(String version) {
        try {
            var parsed = Runtime.Version.parse(version);
            return parsed.compareTo(this.getVersion()) < 0;
        } catch (Exception e) {
            return false;
        }
    }
}

