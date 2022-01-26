package fi.vm.yti.terminology.elasticsearch.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.terminology.api.frontend.elasticqueries.DeepConceptQueryFactory;
import fi.vm.yti.terminology.api.frontend.searchdto.ConceptSimpleDTO;
import fi.vm.yti.terminology.api.frontend.searchdto.DeepSearchHitListDTO;
import fi.vm.yti.terminology.api.frontend.searchdto.TerminologySearchRequest;
import fi.vm.yti.terminology.elasticsearch.EsUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class DeepConceptQueryFactoryTest {

    DeepConceptQueryFactory factory = new DeepConceptQueryFactory(new ObjectMapper());

    @Test
    public void testDeepConceptQuery() throws Exception{
        var jsonExpected = new JSONObject(EsUtils.getJsonString("/es/request/deep_concept_request.json"));

        SearchRequest request = factory.createQuery("test", new String[]{ "DRAFT" }, "fi", false, Set.of("e447089c-dd4e-4744-8b22-4aa97cf6c354"));

        JSONAssert.assertEquals(jsonExpected.toString(), request.source().toString(), JSONCompareMode.LENIENT);
    }

    @Test
    public void testParseDeepConceptQueryResponse() throws Exception {
        SearchResponse searchResponse = EsUtils.getMockResponse("/es/response/concept_response.json");
        TerminologySearchRequest searchRequest = new TerminologySearchRequest();
        searchRequest.setQuery("test");

        Map<String, List<DeepSearchHitListDTO<?>>> parsedResponse = factory.parseResponse(searchResponse, searchRequest);

        // response contains results from two different terminology
        assertEquals(2, parsedResponse.keySet().size());

        String vocabularyId = "ac96b29b-8760-482a-be28-bc162b30e8c9";
        List<DeepSearchHitListDTO<?>> dtos = parsedResponse.get(vocabularyId);

        // one hit in particular terminology
        assertEquals(1, dtos.size());

        DeepSearchHitListDTO dto = dtos.get(0);

        // check concepts properties
        ConceptSimpleDTO conceptSimpleDTO = (ConceptSimpleDTO) dto.getTopHits().get(0);
        assertEquals("http://uri.suomi.fi/terminology/jhs/concept-1234", conceptSimpleDTO.getUri());
        assertEquals("<b>Test</b>", conceptSimpleDTO.getLabel().get("fi"));
    }
}
