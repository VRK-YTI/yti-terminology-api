package fi.vm.yti.terminology.api.v2.dto;

import org.apache.commons.lang3.builder.EqualsBuilder;

import java.util.Objects;

public class LocalizedValueDTO {
    private String language;
    private String value;

    public LocalizedValueDTO(String language, String value) {
        this.language = language;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(language, value);
    }
}
