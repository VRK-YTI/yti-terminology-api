package fi.vm.yti.terminology.api.v2.dto;

import fi.vm.yti.common.dto.ModelMetaDataDTO;

import java.util.Set;

public class TerminologyDTO extends ModelMetaDataDTO {
    private String prefix;
    private Set<String> languages;

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Set<String> getLanguages() {
        return languages;
    }

    public void setLanguages(Set<String> languages) {
        this.languages = languages;
    }
}
