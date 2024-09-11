package fi.vm.yti.terminology.api.v2.dto;

import fi.vm.yti.terminology.api.v2.opensearch.IndexConcept;

public class ConceptSearchResultDTO extends IndexConcept {
    private SimpleTerminologyDTO terminology;

    public SimpleTerminologyDTO getTerminology() {
        return terminology;
    }

    public void setTerminology(SimpleTerminologyDTO terminology) {
        this.terminology = terminology;
    }
}
