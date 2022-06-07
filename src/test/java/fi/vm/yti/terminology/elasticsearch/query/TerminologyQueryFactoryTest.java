package fi.vm.yti.terminology.elasticsearch.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.terminology.api.frontend.elasticqueries.TerminologyQueryFactory;
import fi.vm.yti.terminology.api.frontend.searchdto.TerminologyDTO;
import fi.vm.yti.terminology.api.frontend.searchdto.TerminologySearchRequest;
import fi.vm.yti.terminology.api.frontend.searchdto.TerminologySearchResponse;
import fi.vm.yti.terminology.elasticsearch.EsUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class TerminologyQueryFactoryTest {

    TerminologyQueryFactory factory = new TerminologyQueryFactory(new ObjectMapper());

    @Test
    public void testVocabularySearchRequest() throws Exception {

        var expected = EsUtils.getJsonString("/es/request/vocabulary_request.json");
        var request = new TerminologySearchRequest();
        request.setQuery("test");
        request.setGroups(new String[] { "fa57fa33-4f13-384f-9273-5e4e9b3b7837" });
        request.setOrganizations(new String[] { "f2a0117a-4db8-44e2-91ac-374cd0c792ed" });
        request.setStatuses(new String[] { "VALID", "DRAFT" });

        SearchRequest searchRequest = factory.createQuery(request, false, Collections.emptySet());
        JSONAssert.assertEquals(expected, searchRequest.source().toString(), JSONCompareMode.LENIENT);
    }

    @Test
    public void testParseVocabularySearchResponse() throws Exception {

        var response = EsUtils.getMockResponse("/es/response/vocabulary_response.json");
        var request = new TerminologySearchRequest();
        request.setQuery("mock");

        TerminologySearchResponse terminologySearchResponse = factory.parseResponse(response, request, null);

        assertEquals(1, terminologySearchResponse.getTotalHitCount());
        var dto = terminologySearchResponse.getTerminologies().get(0);

        assertEquals("<b>mock</b> terminology for unit tests", dto.getLabel().get("fi"));
        assertEquals("http://uri.suomi.fi/terminology/kela_test/terminological-vocabulary-0", dto.getUri());
        assertEquals("Test contributor", dto.getContributors().get(0).getLabel().get("fi"));
        assertEquals("DRAFT", dto.getStatus());
        assertEquals("Testipalveluluokka label", dto.getInformationDomains().get(0).getLabel().get("fi"));
    }
}
