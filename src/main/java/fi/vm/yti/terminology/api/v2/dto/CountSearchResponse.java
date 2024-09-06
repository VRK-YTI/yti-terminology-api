package fi.vm.yti.terminology.api.v2.dto;

public class CountSearchResponse {

    private long totalHitCount;
    private CountDTO counts;

    public CountSearchResponse() {
        this.totalHitCount = 0;
        this.counts = new CountDTO();
    }

    public long getTotalHitCount() {
        return totalHitCount;
    }

    public void setTotalHitCount(long totalHitCount) {
        this.totalHitCount = totalHitCount;
    }

    public CountDTO getCounts() {
        return counts;
    }

    public void setCounts(CountDTO counts) {
        this.counts = counts;
    }
}

