package fi.vm.yti.terminology.api.frontend.searchdto;

public class StatusCountSearchResponse {

    private StatusCountDTO counts;

    public StatusCountSearchResponse() {
        this.counts = new StatusCountDTO();
    }

    public StatusCountDTO getCounts() {
        return counts;
    }

    public void setCounts(StatusCountDTO counts) {
        this.counts = counts;
    }
}
