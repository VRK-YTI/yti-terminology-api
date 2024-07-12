package fi.vm.yti.terminology.api.v2.dto;

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
    private Set<ConceptReferenceDTO> references = new HashSet<>();
    private Set<TermDTO> terms = new HashSet<>();

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

    public Set<ConceptReferenceDTO> getReferences() {
        return references;
    }

    public void setReferences(Set<ConceptReferenceDTO> references) {
        this.references = references;
    }

    public Set<TermDTO> getTerms() {
        return terms;
    }

    public void setTerms(Set<TermDTO> terms) {
        this.terms = terms;
    }

    public String getConceptClass() {
        return conceptClass;
    }

    public void setConceptClass(String conceptClass) {
        this.conceptClass = conceptClass;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
