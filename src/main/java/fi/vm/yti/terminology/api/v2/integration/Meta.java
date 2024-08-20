package fi.vm.yti.terminology.api.v2.integration;

public class Meta {

    private long resultCount;
    private long totalResults;

    public long getResultCount() {
        return resultCount;
    }

    public void setResultCount(long resultCount) {
        this.resultCount = resultCount;
    }

    public long getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(long totalResults) {
        this.totalResults = totalResults;
    }
}
