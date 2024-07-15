package fi.vm.yti.terminology.api.v2.dto;

import fi.vm.yti.common.enums.Status;
import fi.vm.yti.terminology.api.v2.enums.*;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;
import java.util.Objects;

public class TermDTO {
    private String identifier;
    private String language;
    private String label;
    private Integer homographNumber;
    private Status status;
    private String termInfo;
    private String scope;
    private String historyNote;
    private String changeNote;
    private String termStyle;
    private TermFamily termFamily;
    private TermConjugation termConjugation;
    private TermEquivalency termEquivalency;
    private WordClass wordClass;
    private List<String> sources = List.of();
    private List<String> editorialNotes = List.of();

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getHomographNumber() {
        return homographNumber;
    }

    public void setHomographNumber(Integer homographNumber) {
        this.homographNumber = homographNumber;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getTermInfo() {
        return termInfo;
    }

    public void setTermInfo(String termInfo) {
        this.termInfo = termInfo;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
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

    public String getTermStyle() {
        return termStyle;
    }

    public void setTermStyle(String termStyle) {
        this.termStyle = termStyle;
    }

    public TermFamily getTermFamily() {
        return termFamily;
    }

    public void setTermFamily(TermFamily termFamily) {
        this.termFamily = termFamily;
    }

    public TermConjugation getTermConjugation() {
        return termConjugation;
    }

    public void setTermConjugation(TermConjugation termConjugation) {
        this.termConjugation = termConjugation;
    }

    public WordClass getWordClass() {
        return wordClass;
    }

    public void setWordClass(WordClass wordClass) {
        this.wordClass = wordClass;
    }

    public TermEquivalency getTermEquivalency() {
        return termEquivalency;
    }

    public void setTermEquivalency(TermEquivalency termEquivalency) {
        this.termEquivalency = termEquivalency;
    }

    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
    }

    public List<String> getEditorialNotes() {
        return editorialNotes;
    }

    public void setEditorialNotes(List<String> editorialNotes) {
        this.editorialNotes = editorialNotes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TermDTO termDTO = (TermDTO) o;
        return Objects.equals(language, termDTO.language)
               && Objects.equals(label, termDTO.label)
               && status == termDTO.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(language, label, status);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
