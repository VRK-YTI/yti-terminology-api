package fi.vm.yti.terminology.api.v2.opensearch;

import fi.vm.yti.terminology.api.v2.TestUtils;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.Set;

import static fi.vm.yti.common.opensearch.OpenSearchUtil.getPayload;

class CountQueryFactoryTest {

    @Test
    void testTerminologyCounts() throws Exception {
        var request = new TerminologySearchRequest();
        var query = CountQueryFactory.createTerminologyCountQuery(request, TestUtils.mockUser, Set.of(), Set.of());

        var expected = TestUtils.getJsonString("/opensearch/terminology-count-query.json");
        JSONAssert.assertEquals(expected, getPayload(query), JSONCompareMode.LENIENT);
    }

    @Test
    void testConceptCountQuery() throws Exception {
        var request = new ConceptSearchRequest();
        var query = CountQueryFactory.createConceptCountQuery(request, false, Set.of());

        var expected = TestUtils.getJsonString("/opensearch/concept-count-query.json");
        JSONAssert.assertEquals(expected, getPayload(query), JSONCompareMode.LENIENT);
    }
}
