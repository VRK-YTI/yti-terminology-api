package fi.vm.yti.terminology.api.importapi.excel;

import fi.vm.yti.terminology.api.model.termed.GenericNode;

import java.util.List;

public class TerminologyImportDTO {

    private String namespace;
    private GenericNode terminologyNode;
    private List<String> languages;

    public TerminologyImportDTO(String namespace, GenericNode terminologyNode, List<String> languages) {
        this.namespace = namespace;
        this.terminologyNode = terminologyNode;
        this.languages = languages;
    }

    public String getNamespace() {
        return namespace;
    }

    public GenericNode getTerminologyNode() {
        return terminologyNode;
    }

    public List<String> getLanguages() {
        return languages;
    }
}
