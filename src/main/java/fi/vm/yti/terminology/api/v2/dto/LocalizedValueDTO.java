package fi.vm.yti.terminology.api.v2.dto;

public class LocalizedValueDTO {
    private String language;
    private String value;

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
}
