package fi.vm.yti.terminology.api.v2.dto;

import fi.vm.yti.common.dto.ModelMetaData;

import java.util.Set;

public class TerminologyDTO extends ModelMetaData {
    private String prefix;
    private Set<String> languages;

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
