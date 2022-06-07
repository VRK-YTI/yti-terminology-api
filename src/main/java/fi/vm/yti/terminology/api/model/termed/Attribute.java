package fi.vm.yti.terminology.api.model.termed;

import java.io.Serializable;

public final class Attribute implements Serializable {

    private final String lang;
    private final String value;
    private final String regex;

    private static final String DEFAULT_ATTRIBUTE_REGEX = "(?s)^.*$";

    // Jackson constructor
    private Attribute() {
        this("", "", "");
    }

    public Attribute(String lang, String value) {
        this(lang, value, DEFAULT_ATTRIBUTE_REGEX);
    }

    public Attribute(String lang, String value, String regex) {
        this.lang = lang;
        this.value = value;
        this.regex = regex;
    }

    public String getLang() {
        return lang;
    }

    public String getValue() {
        return value;
    }

    public String getRegex() {
        return regex;
    }

    public Property asProperty() {
        return new Property(lang, value);
    }
}
