package fi.vm.yti.terminology.api.v2.dto;

import fi.vm.yti.terminology.api.v2.enums.ReferenceType;

public class ConceptReferenceDTO {

    private ReferenceType referenceType;
    private String conceptURI;

    public ReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(ReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public String getConceptURI() {
        return conceptURI;
    }

    public void setConceptURI(String conceptURI) {
        this.conceptURI = conceptURI;
    }
}
