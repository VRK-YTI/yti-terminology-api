package fi.vm.yti.terminology.elasticsearch.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.terminology.api.frontend.elasticqueries.CountQueryFactory;
import fi.vm.yti.terminology.api.frontend.searchdto.CountDTO;
import fi.vm.yti.terminology.api.frontend.searchdto.CountSearchResponse;
import fi.vm.yti.terminology.elasticsearch.EsUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CountQueryFactoryTest {

    CountQueryFactory factory = new CountQueryFactory(new ObjectMapper());

    @Test
    public void testVocabularyCounts() throws Exception {
        String expected = EsUtils.getJsonString("/es/request/vocabulary_count_request.json");

        SearchRequest request = factory.createQuery();

        JSONAssert.assertEquals(expected, request.source().toString(), JSONCompareMode.LENIENT);
    }

    @Test
    public void testParseVocabularyCountResponse() throws Exception {
        SearchResponse response = EsUtils.getMockResponse("/es/response/vocabulary_count_response.json");

        CountSearchResponse countSearchResponse = factory.parseResponse(response);

        assertEquals(8, countSearchResponse.getTotalHitCount());
        Map<String, Long> groups = countSearchResponse.getCounts().getGroups();
        Map<String, Long> statuses = countSearchResponse.getCounts().getStatuses();

        assertEquals(2, groups.keySet().size());
        assertEquals(groups.get("6f505105-5cc8-3293-aff6-64a58114bbe8"), 1L);
        assertEquals(groups.get("9f69e282-b4d9-3cc0-a0d2-e7ccd861266a"), 1L);

        assertEquals(2, statuses.keySet().size());
        assertEquals(7, statuses.get("DRAFT"));
        assertEquals(1, statuses.get("VALID"));
    }
}
