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
    private Set<ConceptReferenceInfoDTO> references = Set.of();
    private List<TermDTO> recommendedTerms = new ArrayList<>();
    private List<TermDTO> synonyms = new ArrayList<>();
    private List<TermDTO> notRecommendedTerms = new ArrayList<>();
    private List<TermDTO> searchTerms = new ArrayList<>();

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

    public Set<ConceptReferenceInfoDTO> getReferences() {
        return references;
    }

    public void setReferences(Set<ConceptReferenceInfoDTO> references) {
        this.references = references;
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
}
