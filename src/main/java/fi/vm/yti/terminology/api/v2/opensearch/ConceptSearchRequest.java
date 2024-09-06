package fi.vm.yti.terminology.api.v2.opensearch;

import fi.vm.yti.common.opensearch.BaseSearchRequest;

public class ConceptSearchRequest extends BaseSearchRequest {
    private String namespace;
    private String excludeNamespace;
    private boolean extendTerminologies;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getExcludeNamespace() {
        return excludeNamespace;
    }

    public void setExcludeNamespace(String excludeNamespace) {
        this.excludeNamespace = excludeNamespace;
    }

    public boolean isExtendTerminologies() {
        return extendTerminologies;
    }

    public void setExtendTerminologies(boolean extendTerminologies) {
        this.extendTerminologies = extendTerminologies;
    }
}
