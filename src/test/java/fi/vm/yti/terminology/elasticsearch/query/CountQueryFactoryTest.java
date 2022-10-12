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
import java.util.UUID;

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
    public void testVocabularyOnlyCounts() throws Exception {
        String expected = EsUtils.getJsonString("/es/request/vocabulary_only_count_request.json");

        SearchRequest request = factory.createVocabularyCountQuery();

        JSONAssert.assertEquals(expected, request.source().toString(), JSONCompareMode.LENIENT);
        assertEquals(request.indices().length, 1);
        assertEquals(request.indices()[0], "vocabularies");
    }

    @Test
    public void testConceptCounts() throws Exception {
        String expected = EsUtils.getJsonString("/es/request/concept_count_request.json");

        SearchRequest request = factory.createConceptCountQuery(UUID.fromString("bab3aa74-a2c2-4750-ad5b-4fd7f007edca"));

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

    @Test
    public void testParseVocabularyOnlyResponse() throws Exception {
        SearchResponse response = EsUtils.getMockResponse("/es/response/vocabulary_only_count_response.json");

        CountSearchResponse countSearchResponse = factory.parseResponse(response);

        assertEquals(8, countSearchResponse.getTotalHitCount());
        Map<String, Long> groups = countSearchResponse.getCounts().getGroups();
        Map<String, Long> statuses = countSearchResponse.getCounts().getStatuses();
        Map<String, Long> categories = countSearchResponse.getCounts().getCategories();
        Map<String, Long> languages = countSearchResponse.getCounts().getLanguages();

        assertEquals(2, groups.keySet().size());
        assertEquals(groups.get("6f505105-5cc8-3293-aff6-64a58114bbe8"), 1L);
        assertEquals(groups.get("9f69e282-b4d9-3cc0-a0d2-e7ccd861266a"), 1L);

        assertEquals(2, statuses.keySet().size());
        assertEquals(7, statuses.get("DRAFT"));
        assertEquals(1, statuses.get("VALID"));

        assertEquals(2, categories.get("TerminologicalVocabulary"));
        assertEquals(0, categories.get("Concept"));
        assertEquals(0, categories.get("OtherVocabulary"));
        assertEquals(0, categories.get("Collection"));

        assertEquals(13, languages.get("fi"));
        assertEquals(10, languages.get("sv"));
        assertEquals(8, languages.get("en"));
    }
}
