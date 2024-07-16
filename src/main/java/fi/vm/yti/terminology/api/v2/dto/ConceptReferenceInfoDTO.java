package fi.vm.yti.terminology.api.v2.dto;

import java.util.Map;

public class ConceptReferenceInfoDTO {
    private String conceptURI;
    private Map<String, String> label;

    public String getConceptURI() {
        return conceptURI;
    }

    public void setConceptURI(String conceptURI) {
        this.conceptURI = conceptURI;
    }

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }
}
