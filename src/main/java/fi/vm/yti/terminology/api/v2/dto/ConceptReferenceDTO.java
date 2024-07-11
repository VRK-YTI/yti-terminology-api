package fi.vm.yti.terminology.api.v2.dto;

import fi.vm.yti.terminology.api.v2.enums.ReferenceType;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConceptReferenceDTO that = (ConceptReferenceDTO) o;
        return referenceType == that.referenceType && Objects.equals(conceptURI, that.conceptURI);
    }

    @Override
    public int hashCode() {
        return Objects.hash(referenceType, conceptURI);
    }
}
