package fi.vm.yti.terminology.api.v2.integration;

import java.util.ArrayList;
import java.util.List;

public class IntegrationResponse {
    private Meta meta;

    private List<IntegrationResult> results = new ArrayList<>();

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public List<IntegrationResult> getResults() {
        return results;
    }

    public void setResults(List<IntegrationResult> results) {
        this.results = results;
    }
}
