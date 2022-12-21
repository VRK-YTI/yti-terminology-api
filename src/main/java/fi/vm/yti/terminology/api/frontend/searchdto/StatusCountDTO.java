package fi.vm.yti.terminology.api.frontend.searchdto;

import java.util.Collections;
import java.util.Map;

public class StatusCountDTO {

    private Map<String, Long> concepts;

    private Map<String, Long> terms;

    public StatusCountDTO() {
        this.concepts = Collections.emptyMap();
        this.terms = Collections.emptyMap();
    }

    public StatusCountDTO(
            final Map<String, Long> concepts,
            final Map<String, Long> terms) {
        this.concepts = concepts;
        this.terms = terms;
    }

    public Map<String, Long> getConcepts() {
        return concepts;
    }

    public void setConcepts(Map<String, Long> concepts) {
        this.concepts = concepts;
    }

    public Map<String, Long> getTerms() {
        return terms;
    }

    public void setTerms(Map<String, Long> terms) {
        this.terms = terms;
    }

}
