package fi.vm.yti.terminology.api.v2.enums;

import java.util.HashMap;
import java.util.Map;

public enum TermType {
    RECOMMENDED("prefLabel"),
    SYNONYM("altLabel"),
    NOT_RECOMMENDED("notRecommendedSynonym"),
    SEARCH_TERM("hiddenLabel");

    private final String property;
    private static final Map<String, TermType> lookup = new HashMap<>();

    static {
        for (var value : TermType.values()) {
            lookup.put(value.getProperty(), value);
        }
    }

    TermType(String property) {
        this.property = property;
    }

    public String getProperty() {
        return property;
    }

    public static TermType getByPropertyName(String property) {
        return lookup.get(property);
    }

}
