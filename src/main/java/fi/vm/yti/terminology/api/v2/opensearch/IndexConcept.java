package fi.vm.yti.terminology.api.v2.opensearch;

import fi.vm.yti.common.opensearch.IndexBase;

import java.util.List;
import java.util.Map;

public class IndexConcept extends IndexBase {
    private String namespace;
    private String identifier;
    private Map<String, String> definition;
    private List<String> altLabel;
    private List<String> searchTerm;
    private List<String> notRecommendedSynonym;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Map<String, String> getDefinition() {
        return definition;
    }

    public void setDefinition(Map<String, String> definition) {
        this.definition = definition;
    }

    public List<String> getAltLabel() {
        return altLabel;
    }

    public void setAltLabel(List<String> altLabel) {
        this.altLabel = altLabel;
    }

    public List<String> getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(List<String> searchTerm) {
        this.searchTerm = searchTerm;
    }

    public List<String> getNotRecommendedSynonym() {
        return notRecommendedSynonym;
    }

    public void setNotRecommendedSynonym(List<String> notRecommendedSynonym) {
        this.notRecommendedSynonym = notRecommendedSynonym;
    }
}
