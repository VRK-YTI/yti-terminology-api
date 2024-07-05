package fi.vm.yti.terminology.api.v2.opensearch;

import fi.vm.yti.common.opensearch.IndexBase;

import java.util.List;
import java.util.Map;

public class IndexConcept extends IndexBase {
    private String namespace;
    private Map<String, String> definition;
    private Map<String, List<String>> altLabel;
    private Map<String, List<String>> searchTerm;
    private Map<String, List<String>> notRecommendedSynonym;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Map<String, String> getDefinition() {
        return definition;
    }

    public void setDefinition(Map<String, String> definition) {
        this.definition = definition;
    }

    public Map<String, List<String>> getAltLabel() {
        return altLabel;
    }

    public void setAltLabel(Map<String, List<String>> altLabel) {
        this.altLabel = altLabel;
    }

    public Map<String, List<String>> getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(Map<String, List<String>> searchTerm) {
        this.searchTerm = searchTerm;
    }

    public Map<String, List<String>> getNotRecommendedSynonym() {
        return notRecommendedSynonym;
    }

    public void setNotRecommendedSynonym(Map<String, List<String>> notRecommendedSynonym) {
        this.notRecommendedSynonym = notRecommendedSynonym;
    }
}
