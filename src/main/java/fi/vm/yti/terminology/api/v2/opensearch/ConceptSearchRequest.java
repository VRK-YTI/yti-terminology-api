package fi.vm.yti.terminology.api.v2.opensearch;

import fi.vm.yti.common.opensearch.BaseSearchRequest;

import java.util.Set;
import java.util.UUID;

public class ConceptSearchRequest extends BaseSearchRequest {
    private String namespace;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
