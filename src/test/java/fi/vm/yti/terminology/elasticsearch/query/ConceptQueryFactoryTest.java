package fi.vm.yti.terminology.elasticsearch.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.terminology.api.frontend.elasticqueries.ConceptQueryFactory;
import fi.vm.yti.terminology.api.frontend.searchdto.ConceptSearchRequest;
import fi.vm.yti.terminology.elasticsearch.EsUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

public class ConceptQueryFactoryTest {

    ConceptQueryFactory factory = new ConceptQueryFactory(new ObjectMapper(), "testNamespace");


    @Test
    public void createQuery() throws Exception {
        var jsonExpected = new JSONObject(EsUtils.getJsonString("/es/request/concept_request.json"));

        ConceptSearchRequest request = new ConceptSearchRequest();
        request.setQuery("test");

        SearchRequest searchRequest = factory.createQuery(request, true, limitToTheseTerminologyIds -> null);

        JSONAssert.assertEquals(jsonExpected.toString(), searchRequest.source().toString(), JSONCompareMode.LENIENT);

    }
}
