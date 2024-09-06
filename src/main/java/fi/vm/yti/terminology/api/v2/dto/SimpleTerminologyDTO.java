package fi.vm.yti.terminology.api.v2.dto;

import java.util.Map;

public class SimpleTerminologyDTO {
    private String prefix;
    private Map<String, String> label = Map.of();

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }
}
