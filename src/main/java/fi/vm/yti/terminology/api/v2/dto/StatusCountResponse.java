package fi.vm.yti.terminology.api.v2.dto;

import java.util.Collections;
import java.util.Map;

public class StatusCountResponse {
    private Map<String, Long> concepts;

    private Map<String, Long> terms;

    public StatusCountResponse() {
        this.concepts = Collections.emptyMap();
        this.terms = Collections.emptyMap();
    }

    public StatusCountResponse(
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
