package fi.vm.yti.terminology.api.v2.opensearch;

import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.opensearch.SearchResponseDTO;
import fi.vm.yti.terminology.api.v2.dto.ConceptSearchResultDTO;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import fi.vm.yti.terminology.api.v2.TestUtils;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static fi.vm.yti.common.opensearch.OpenSearchUtil.getPayload;
import static org.springframework.test.util.AssertionErrors.assertEquals;

public class TerminologyQueryFactoryTest {

    @Test
    void shouldCreateTerminologyQuery() throws Exception {
        var request = new TerminologySearchRequest();
        request.setQuery("test");
        request.setGroups(Set.of("P11", "P1"));
        request.setPageSize(100);
        request.setStatus(Set.of(Status.VALID));
        var terminologyQuery = TerminologyQueryFactory.createTerminologyQuery(request, false, null);
        var expected = TestUtils.getJsonString("/opensearch/terminology-request.json");
        JSONAssert.assertEquals(expected, getPayload(terminologyQuery), JSONCompareMode.LENIENT);

        assertEquals("Page from value not matching", 0, terminologyQuery.from());
        assertEquals("Page size value not matching", 100, terminologyQuery.size());
    }

    @Test
    void shouldCreateTerminologyQueryWithDeepSearch() throws Exception {
        var request = new TerminologySearchRequest();
        request.setQuery("test");
        request.setGroups(Set.of("P11", "P1"));
        request.setPageSize(100);

        var deepSearchHits = new SearchResponseDTO<ConceptSearchResultDTO>();
        var conceptSearchResult1 = new ConceptSearchResultDTO();
        conceptSearchResult1.setUri("https://iri.test/terminology/terminology1/concept1");
        var conceptSearchResult2 = new ConceptSearchResultDTO();
        conceptSearchResult2.setUri("https://iri.test/terminology/terminology1/concept2");
        deepSearchHits.setResponseObjects(List.of(conceptSearchResult1, conceptSearchResult2));

        var testUuid = UUID.fromString("d1f9f5fc-3aca-11ef-8006-7ef97ea86967");

        var terminologyQuery = TerminologyQueryFactory.createTerminologyQuery(
                request,
                false,
                deepSearchHits,
                Set.of(testUuid));
        var expected = TestUtils.getJsonString("/opensearch/terminology-deep-request.json");
        JSONAssert.assertEquals(expected, getPayload(terminologyQuery), JSONCompareMode.LENIENT);

        assertEquals("Page from value not matching", 0, terminologyQuery.from());
        assertEquals("Page size value not matching", 100, terminologyQuery.size());
    }

    @Test
    void shouldCreateTerminologyQueryWithDeepSearchAsSuperuser() throws Exception {
        var request = new TerminologySearchRequest();
        request.setQuery("test");
        request.setGroups(Set.of("P11", "P1"));
        request.setPageSize(100);

        var deepSearchHits = new SearchResponseDTO<ConceptSearchResultDTO>();
        var conceptSearchResult1 = new ConceptSearchResultDTO();
        conceptSearchResult1.setUri("https://iri.test/terminology/terminology1/concept1");
        var conceptSearchResult2 = new ConceptSearchResultDTO();
        conceptSearchResult2.setUri("https://iri.test/terminology/terminology1/concept2");
        deepSearchHits.setResponseObjects(List.of(conceptSearchResult1, conceptSearchResult2));

        var testUuid = UUID.fromString("d1f9f5fc-3aca-11ef-8006-7ef97ea86967");

        var terminologyQuery = TerminologyQueryFactory.createTerminologyQuery(
                request,
                true,
                deepSearchHits,
                null);
        var expected = TestUtils.getJsonString("/opensearch/terminology-deep-superuser-request.json");
        JSONAssert.assertEquals(expected, getPayload(terminologyQuery), JSONCompareMode.LENIENT);

        assertEquals("Page from value not matching", 0, terminologyQuery.from());
        assertEquals("Page size value not matching", 100, terminologyQuery.size());
    }
}
