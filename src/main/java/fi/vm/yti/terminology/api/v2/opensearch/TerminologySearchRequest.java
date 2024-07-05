package fi.vm.yti.terminology.api.v2.opensearch;

import fi.vm.yti.common.opensearch.BaseSearchRequest;

import java.util.Set;
import java.util.UUID;

public class TerminologySearchRequest extends BaseSearchRequest {
    private boolean searchConcepts;

    private Set<String> groups;

    private Set<UUID> organizations;

    private Set<String> languages;

    public boolean isSearchConcepts() {
        return searchConcepts;
    }

    public void setSearchConcepts(boolean searchConcepts) {
        this.searchConcepts = searchConcepts;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    public Set<UUID> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(Set<UUID> organizations) {
        this.organizations = organizations;
    }

    public Set<String> getLanguages() {
        return languages;
    }

    public void setLanguages(Set<String> languages) {
        this.languages = languages;
    }
}
