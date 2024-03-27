package fi.vm.yti.terminology.api.v2.dto;

import java.util.List;
import java.util.Map;

import fi.vm.yti.common.dto.BaseDTO;

public class ConceptDTO extends BaseDTO {
    private Map<String, String> definition;
    private List<TermDTO> terms;

    public Map<String, String> getDefinition() {
        return definition;
    }

    public void setDefinition(Map<String, String> definition) {
        this.definition = definition;
    }

    public List<TermDTO> getTerms() {
        return terms;
    }

    public void setTerms(List<TermDTO> terms) {
        this.terms = terms;
    }
}
