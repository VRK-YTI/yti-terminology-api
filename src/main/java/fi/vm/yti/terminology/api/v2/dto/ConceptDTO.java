package fi.vm.yti.terminology.api.v2.dto;

import java.util.List;
import java.util.Map;

import fi.vm.yti.common.dto.ResourceBaseDTO;

public class ConceptDTO extends ResourceBaseDTO {
    private Map<String, String> definition;

    private List<TermDTO> terms;
}
