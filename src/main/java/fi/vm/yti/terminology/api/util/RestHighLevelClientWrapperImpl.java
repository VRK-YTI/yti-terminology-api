package fi.vm.yti.terminology.api.util;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

import java.io.IOException;

// Wrapper for RestHighLevelClient that can be mocked in tests
public class RestHighLevelClientWrapperImpl implements RestHighLevelClientWrapper {

    private final RestHighLevelClient client;

    public RestHighLevelClientWrapperImpl(RestHighLevelClient client) {
        this.client = client;
    }

    public SearchResponse search(
            SearchRequest searchRequest,
            RequestOptions options) throws IOException {
        return client.search(searchRequest, options);
    }

    public RestClient getLowLevelClient() {
        return this.client.getLowLevelClient();
    }
}
