package fi.vm.yti.terminology.api.v2.dto;

import fi.vm.yti.terminology.api.v2.opensearch.IndexTerminology;

import java.util.List;

public class TerminologySearchResultDTO extends IndexTerminology {
    public TerminologySearchResultDTO() {
        this.setMatchingConcepts(new java.util.ArrayList<>());
    }

    // matching concepts from deep search
    private List<ConceptSearchResultDTO> matchingConcepts;

    public List<ConceptSearchResultDTO> getMatchingConcepts() {
        return matchingConcepts;
    }

    public void setMatchingConcepts(List<ConceptSearchResultDTO> matchingConcepts) {
        this.matchingConcepts = matchingConcepts;
    }

    public void addMatchingConcept(ConceptSearchResultDTO concept) {
        this.matchingConcepts.add(concept);
    }
}
