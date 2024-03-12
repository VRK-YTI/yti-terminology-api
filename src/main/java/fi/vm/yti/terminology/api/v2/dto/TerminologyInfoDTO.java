package fi.vm.yti.terminology.api.v2.dto;

import fi.vm.yti.common.dto.ResourceCommonDTO;

public class TerminologyInfoDTO extends ResourceCommonDTO {
    private String prefix;

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
