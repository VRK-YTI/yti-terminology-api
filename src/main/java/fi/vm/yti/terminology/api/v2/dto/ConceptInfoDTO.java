package fi.vm.yti.terminology.api.v2.dto;

import fi.vm.yti.common.dto.LinkDTO;
import fi.vm.yti.common.dto.ResourceCommonInfoDTO;
import fi.vm.yti.common.enums.Status;

import java.util.*;

public class ConceptInfoDTO extends ResourceCommonInfoDTO {
    private String identifier;
    private Map<String, String> definition = Map.of();
    private String subjectArea;
    private List<LocalizedValueDTO> notes = List.of();
    private List<LocalizedValueDTO> examples = List.of();
    private Status status;
    private List<String> sources = List.of();
    private List<LinkDTO> links = List.of();
    private String changeNote;
    private String historyNote;
    private String conceptClass;
    private List<String> editorialNotes = List.of();
    private Set<ConceptReferenceInfoDTO> broader = new LinkedHashSet<>();
    private Set<ConceptReferenceInfoDTO> narrower = new LinkedHashSet<>();
    private Set<ConceptReferenceInfoDTO> isPartOf = new LinkedHashSet<>();
    private Set<ConceptReferenceInfoDTO> hasPart = new LinkedHashSet<>();
    private Set<ConceptReferenceInfoDTO> related = new LinkedHashSet<>();
    private Set<ConceptReferenceInfoDTO> broadMatch = new LinkedHashSet<>();
    private Set<ConceptReferenceInfoDTO> narrowMatch = new LinkedHashSet<>();
    private Set<ConceptReferenceInfoDTO> exactMatch = new LinkedHashSet<>();
    private Set<ConceptReferenceInfoDTO> closeMatch = new LinkedHashSet<>();
    private Set<ConceptReferenceInfoDTO> relatedMatch = new LinkedHashSet<>();
    private List<TermDTO> recommendedTerms = new ArrayList<>();
    private List<TermDTO> synonyms = new ArrayList<>();
    private List<TermDTO> notRecommendedTerms = new ArrayList<>();
    private List<TermDTO> searchTerms = new ArrayList<>();
    private List<ConceptReferenceInfoDTO> memberOf = new ArrayList<>();

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Map<String, String> getDefinition() {
        return definition;
    }

    public void setDefinition(Map<String, String> definition) {
        this.definition = definition;
    }

    public String getSubjectArea() {
        return subjectArea;
    }

    public void setSubjectArea(String subjectArea) {
        this.subjectArea = subjectArea;
    }

    public List<LocalizedValueDTO> getNotes() {
        return notes;
    }

    public void setNotes(List<LocalizedValueDTO> notes) {
        this.notes = notes;
    }

    public List<LocalizedValueDTO> getExamples() {
        return examples;
    }

    public void setExamples(List<LocalizedValueDTO> examples) {
        this.examples = examples;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
    }

    public List<LinkDTO> getLinks() {
        return links;
    }

    public void setLinks(List<LinkDTO> links) {
        this.links = links;
    }

    public String getChangeNote() {
        return changeNote;
    }

    public void setChangeNote(String changeNote) {
        this.changeNote = changeNote;
    }

    public String getHistoryNote() {
        return historyNote;
    }

    public void setHistoryNote(String historyNote) {
        this.historyNote = historyNote;
    }

    public String getConceptClass() {
        return conceptClass;
    }

    public void setConceptClass(String conceptClass) {
        this.conceptClass = conceptClass;
    }

    public List<String> getEditorialNotes() {
        return editorialNotes;
    }

    public void setEditorialNotes(List<String> editorialNotes) {
        this.editorialNotes = editorialNotes;
    }

    public Set<ConceptReferenceInfoDTO> getBroader() {
        return broader;
    }

    public void setBroader(Set<ConceptReferenceInfoDTO> broader) {
        this.broader = broader;
    }

    public Set<ConceptReferenceInfoDTO> getNarrower() {
        return narrower;
    }

    public void setNarrower(Set<ConceptReferenceInfoDTO> narrower) {
        this.narrower = narrower;
    }

    public Set<ConceptReferenceInfoDTO> getIsPartOf() {
        return isPartOf;
    }

    public void setIsPartOf(Set<ConceptReferenceInfoDTO> isPartOf) {
        this.isPartOf = isPartOf;
    }

    public Set<ConceptReferenceInfoDTO> getHasPart() {
        return hasPart;
    }

    public void setHasPart(Set<ConceptReferenceInfoDTO> hasPart) {
        this.hasPart = hasPart;
    }

    public Set<ConceptReferenceInfoDTO> getRelated() {
        return related;
    }

    public void setRelated(Set<ConceptReferenceInfoDTO> related) {
        this.related = related;
    }

    public Set<ConceptReferenceInfoDTO> getBroadMatch() {
        return broadMatch;
    }

    public void setBroadMatch(Set<ConceptReferenceInfoDTO> broadMatch) {
        this.broadMatch = broadMatch;
    }

    public Set<ConceptReferenceInfoDTO> getNarrowMatch() {
        return narrowMatch;
    }

    public void setNarrowMatch(Set<ConceptReferenceInfoDTO> narrowMatch) {
        this.narrowMatch = narrowMatch;
    }

    public Set<ConceptReferenceInfoDTO> getExactMatch() {
        return exactMatch;
    }

    public void setExactMatch(Set<ConceptReferenceInfoDTO> exactMatch) {
        this.exactMatch = exactMatch;
    }

    public Set<ConceptReferenceInfoDTO> getCloseMatch() {
        return closeMatch;
    }

    public void setCloseMatch(Set<ConceptReferenceInfoDTO> closeMatch) {
        this.closeMatch = closeMatch;
    }

    public Set<ConceptReferenceInfoDTO> getRelatedMatch() {
        return relatedMatch;
    }

    public void setRelatedMatch(Set<ConceptReferenceInfoDTO> relatedMatch) {
        this.relatedMatch = relatedMatch;
    }

    public List<TermDTO> getRecommendedTerms() {
        return recommendedTerms;
    }

    public void setRecommendedTerms(List<TermDTO> recommendedTerms) {
        this.recommendedTerms = recommendedTerms;
    }

    public List<TermDTO> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(List<TermDTO> synonyms) {
        this.synonyms = synonyms;
    }

    public List<TermDTO> getNotRecommendedTerms() {
        return notRecommendedTerms;
    }

    public void setNotRecommendedTerms(List<TermDTO> notRecommendedTerms) {
        this.notRecommendedTerms = notRecommendedTerms;
    }

    public List<TermDTO> getSearchTerms() {
        return searchTerms;
    }

    public void setSearchTerms(List<TermDTO> searchTerms) {
        this.searchTerms = searchTerms;
    }

    public List<ConceptReferenceInfoDTO> getMemberOf() {
        return memberOf;
    }

    public void setMemberOf(List<ConceptReferenceInfoDTO> memberOf) {
        this.memberOf = memberOf;
    }
}
