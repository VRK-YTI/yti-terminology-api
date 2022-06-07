package fi.vm.yti.terminology.api.util;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;

import java.io.IOException;

// Wrapper for RestHighLevelClient that can be mocked in tests
public interface RestHighLevelClientWrapper {
    SearchResponse search(
            SearchRequest searchRequest,
            RequestOptions options) throws IOException;

    RestClient getLowLevelClient();
}
