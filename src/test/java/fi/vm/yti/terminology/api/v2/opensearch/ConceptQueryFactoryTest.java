package fi.vm.yti.terminology.api.v2.opensearch;

import fi.vm.yti.common.enums.Status;
import fi.vm.yti.terminology.api.v2.TestUtils;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.Set;

import static fi.vm.yti.common.opensearch.OpenSearchUtil.getPayload;
import static org.springframework.test.util.AssertionErrors.assertEquals;

class ConceptQueryFactoryTest {
    @Test
    void shouldCreateConceptQuery() throws Exception {
        var request = new ConceptSearchRequest();
        request.setQuery("test");
        request.setPageSize(100);
        request.setStatus(Set.of(
                Status.VALID,
                Status.DRAFT,
                Status.INCOMPLETE));
        var conceptQuery = ConceptQueryFactory.createConceptQuery(
                request,
                false,
                null);
        var expected = TestUtils.getJsonString("/opensearch/concept-request.json");
        JSONAssert.assertEquals(expected, getPayload(conceptQuery), JSONCompareMode.LENIENT);

        assertEquals("Page from value not matching", 0, conceptQuery.from());
        assertEquals("Page size value not matching", 100, conceptQuery.size());
    }

    @Test
    void shouldCreateConceptQueryWithExceptions() throws Exception {
        var request = new ConceptSearchRequest();
        request.setQuery("test");
        request.setPageSize(100);
        request.setStatus(Set.of(
                Status.VALID,
                Status.DRAFT,
                Status.INCOMPLETE));
        var conceptQuery = ConceptQueryFactory.createConceptQuery(
                request,
                false,
                Set.of("https://iri.test/terminology/terminology1/concept"));
        var expected = TestUtils.getJsonString("/opensearch/concept-exceptions-request.json");
        JSONAssert.assertEquals(expected, getPayload(conceptQuery), JSONCompareMode.LENIENT);

        assertEquals("Page from value not matching", 0, conceptQuery.from());
        assertEquals("Page size value not matching", 100, conceptQuery.size());
    }

    @Test
    void shouldCreateConceptQueryAsSuperuser() throws Exception {
        var request = new ConceptSearchRequest();
        request.setQuery("test");
        request.setPageSize(100);
        request.setStatus(Set.of(
                Status.VALID,
                Status.DRAFT,
                Status.INCOMPLETE));
        var conceptQuery = ConceptQueryFactory.createConceptQuery(
                request,
                true,
                null);
        var expected = TestUtils.getJsonString("/opensearch/concept-superuser-request.json");
        JSONAssert.assertEquals(expected, getPayload(conceptQuery), JSONCompareMode.LENIENT);

        assertEquals("Page from value not matching", 0, conceptQuery.from());
        assertEquals("Page size value not matching", 100, conceptQuery.size());
    }

    @Test
    void shouldCreateConceptQueryWithSorting() throws Exception {
        var request = new ConceptSearchRequest();
        request.setPageSize(100);
        request.setNamespace("https://iri.suomi.fi/terminology/test/");
        request.setStatus(Set.of(Status.VALID));
        var conceptQuery = ConceptQueryFactory.createConceptQuery(
                request,
                true,
                null);
        var expected = TestUtils.getJsonString("/opensearch/concept-query-with-sort.json");
        JSONAssert.assertEquals(expected, getPayload(conceptQuery), JSONCompareMode.LENIENT);
    }
}
