package fi.vm.yti.terminology.api.v2.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fi.vm.yti.common.dto.LinkDTO;
import fi.vm.yti.common.enums.Status;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.*;

public class ConceptDTO {
    private String identifier;
    private Map<String, String> definition = new HashMap<>();
    private String subjectArea;
    private List<LocalizedValueDTO> notes = new ArrayList<>();
    private List<LocalizedValueDTO> examples = new ArrayList<>();
    private Status status;
    private List<String> sources = new ArrayList<>();
    private List<LinkDTO> links = List.of();
    private String changeNote;
    private String historyNote;
    private String conceptClass;
    private List<String> editorialNotes = new ArrayList<>();
    private List<TermDTO> recommendedTerms = new ArrayList<>();
    private List<TermDTO> synonyms = new ArrayList<>();
    private List<TermDTO> notRecommendedTerms = new ArrayList<>();
    private List<TermDTO> searchTerms = new ArrayList<>();

    @JsonDeserialize(as=LinkedHashSet.class)
    private Set<String> broader = new LinkedHashSet<>();
    @JsonDeserialize(as=LinkedHashSet.class)
    private Set<String> narrower = new LinkedHashSet<>();
    @JsonDeserialize(as=LinkedHashSet.class)
    private Set<String> isPartOf = new LinkedHashSet<>();
    @JsonDeserialize(as=LinkedHashSet.class)
    private Set<String> hasPart = new LinkedHashSet<>();
    @JsonDeserialize(as=LinkedHashSet.class)
    private Set<String> related = new LinkedHashSet<>();
    @JsonDeserialize(as=LinkedHashSet.class)
    private Set<String> broadMatch = new LinkedHashSet<>();
    @JsonDeserialize(as=LinkedHashSet.class)
    private Set<String> narrowMatch = new LinkedHashSet<>();
    @JsonDeserialize(as=LinkedHashSet.class)
    private Set<String> exactMatch = new LinkedHashSet<>();
    private Set<String> closeMatch = new LinkedHashSet<>();
    @JsonDeserialize(as=LinkedHashSet.class)
    private Set<String> relatedMatch = new LinkedHashSet<>();

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

    public String getHistoryNote() {
        return historyNote;
    }

    public void setHistoryNote(String historyNote) {
        this.historyNote = historyNote;
    }

    public String getChangeNote() {
        return changeNote;
    }

    public void setChangeNote(String changeNote) {
        this.changeNote = changeNote;
    }

    public List<String> getEditorialNotes() {
        return editorialNotes;
    }

    public void setEditorialNotes(List<String> editorialNotes) {
        this.editorialNotes = editorialNotes;
    }

    public String getConceptClass() {
        return conceptClass;
    }

    public void setConceptClass(String conceptClass) {
        this.conceptClass = conceptClass;
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

    public Set<String> getBroader() {
        return broader;
    }

    public void setBroader(Set<String> broader) {
        this.broader = broader;
    }

    public Set<String> getNarrower() {
        return narrower;
    }

    public void setNarrower(Set<String> narrower) {
        this.narrower = narrower;
    }

    public Set<String> getIsPartOf() {
        return isPartOf;
    }

    public void setIsPartOf(Set<String> isPartOf) {
        this.isPartOf = isPartOf;
    }

    public Set<String> getHasPart() {
        return hasPart;
    }

    public void setHasPart(Set<String> hasPart) {
        this.hasPart = hasPart;
    }

    public Set<String> getRelated() {
        return related;
    }

    public void setRelated(Set<String> related) {
        this.related = related;
    }

    public Set<String> getBroadMatch() {
        return broadMatch;
    }

    public void setBroadMatch(Set<String> broadMatch) {
        this.broadMatch = broadMatch;
    }

    public Set<String> getNarrowMatch() {
        return narrowMatch;
    }

    public void setNarrowMatch(Set<String> narrowMatch) {
        this.narrowMatch = narrowMatch;
    }

    public Set<String> getExactMatch() {
        return exactMatch;
    }

    public void setExactMatch(Set<String> exactMatch) {
        this.exactMatch = exactMatch;
    }

    public Set<String> getCloseMatch() {
        return closeMatch;
    }

    public void setCloseMatch(Set<String> closeMatch) {
        this.closeMatch = closeMatch;
    }

    public Set<String> getRelatedMatch() {
        return relatedMatch;
    }

    public void setRelatedMatch(Set<String> relatedMatch) {
        this.relatedMatch = relatedMatch;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
