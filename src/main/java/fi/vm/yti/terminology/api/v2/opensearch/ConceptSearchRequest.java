package fi.vm.yti.terminology.api.v2.opensearch;

import fi.vm.yti.common.opensearch.BaseSearchRequest;

import java.util.Set;

public class ConceptSearchRequest extends BaseSearchRequest {
    private String namespace;
    private Set<String> namespaces = Set.of();
    private String excludeNamespace;
    private boolean extendTerminologies;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Set<String> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(Set<String> namespaces) {
        this.namespaces = namespaces;
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
