package fi.vm.yti.terminology.api.v2.dto;

import java.util.Map;

public class ConceptReferenceInfoDTO {
    private String referenceURI;
    private Map<String, String> label;
    private Map<String, String> terminologyLabel = Map.of();
    private String prefix;
    private String identifier;

    public String getReferenceURI() {
        return referenceURI;
    }

    public void setReferenceURI(String referenceURI) {
        this.referenceURI = referenceURI;
    }

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    public Map<String, String> getTerminologyLabel() {
        return terminologyLabel;
    }

    public void setTerminologyLabel(Map<String, String> terminologyLabel) {
        this.terminologyLabel = terminologyLabel;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
