package fi.vm.yti.terminology.api.v2.dto;

import fi.vm.yti.terminology.api.v2.enums.ReferenceType;

import java.util.Map;

public class ConceptReferenceInfoDTO {
    private String conceptURI;
    private ReferenceType referenceType;
    private Map<String, String> label;

    public String getConceptURI() {
        return conceptURI;
    }

    public void setConceptURI(String conceptURI) {
        this.conceptURI = conceptURI;
    }

    public ReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(ReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }
}
