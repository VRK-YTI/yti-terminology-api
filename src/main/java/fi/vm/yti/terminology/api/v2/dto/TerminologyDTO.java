package fi.vm.yti.terminology.api.v2.dto;

import fi.vm.yti.common.dto.MetaDataDTO;

import java.util.Set;

public class TerminologyDTO extends MetaDataDTO {
    private Set<String> languages;

    public Set<String> getLanguages() {
        return languages;
    }

    public void setLanguages(Set<String> languages) {
        this.languages = languages;
    }
}
