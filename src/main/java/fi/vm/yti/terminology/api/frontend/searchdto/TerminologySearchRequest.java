package fi.vm.yti.terminology.api.frontend.searchdto;

public class TerminologySearchRequest {

    private String query;

    private boolean searchConcepts;

    private String[] statuses;

    private String[] groups;

    private String[] types;

    private String[] organizations;

    private String prefLang;

    private String language;

    private Integer pageSize;
    private Integer pageFrom;

    public String getQuery() {
        return query;
    }

    public void setQuery(final String query) {
        this.query = query;
    }

    public boolean isSearchConcepts() {
        return searchConcepts;
    }

    public void setSearchConcepts(final boolean searchConcepts) {
        this.searchConcepts = searchConcepts;
    }

    public String[] getStatuses() {
        return statuses;
    }

    public void setStatuses(String[] statuses) {
        this.statuses = statuses;
    }

    public String[] getGroups() {
        return groups;
    }

    public void setGroups(String[] groups) {
        this.groups = groups;
    }

    public String[] getTypes() {
        return types;
    }

    public void setTypes(String[] types) {
        this.types = types;
    }

    public String[] getOrganizations() {
        return organizations;
    }

    public void setOrganizations(String[] organizations) {
        this.organizations = organizations;
    }

    public String getPrefLang() {
        return prefLang;
    }

    public void setPrefLang(final String prefLang) {
        this.prefLang = prefLang;
    }


    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(final Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getPageFrom() {
        return pageFrom;
    }

    public void setPageFrom(final Integer pageFrom) {
        this.pageFrom = pageFrom;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public String toString() {
        return "TerminologySearchRequest{" +
            "query='" + query + '\'' +
            ", searchConcepts=" + searchConcepts +
            ", prefLang=" + prefLang +
            ", pageSize=" + pageSize +
            ", pageFrom=" + pageFrom +
            '}';
    }
}
