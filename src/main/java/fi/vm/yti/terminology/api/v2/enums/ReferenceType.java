package fi.vm.yti.terminology.api.v2.enums;

import java.util.HashMap;
import java.util.Map;

public enum ReferenceType {
    BROADER("broader"),
    NARROWER("narrower"),
    IS_PART_OF("isPartOf"),
    HAS_PART("hasPart"),
    RELATED("related"),
    BROAD_MATCH("broadMatch"),
    NARROW_MATCH("narrowMatch"),
    CLOSE_MATCH("closeMatch"),
    EXACT_MATCH("exactMatch"),
    RELATED_MATCH("relatedMatch");

    private final String property;

    private static final Map<String, ReferenceType> lookup = new HashMap<>();

    static {
        for (var value : ReferenceType.values()) {
            lookup.put(value.getProperty(), value);
        }
    }

    ReferenceType(String property) {
        this.property = property;
    }

    public String getProperty() {
        return property;
    }

    public static ReferenceType getByPropertyName(String property) {
        return lookup.get(property);
    }
}
