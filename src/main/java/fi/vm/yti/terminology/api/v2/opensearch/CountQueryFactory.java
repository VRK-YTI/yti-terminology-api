package fi.vm.yti.terminology.api.v2.opensearch;

import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.v2.service.IndexService;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.AggregationBuilders;
import org.opensearch.client.opensearch.core.SearchRequest;

import java.util.Set;
import java.util.UUID;

import static fi.vm.yti.common.opensearch.OpenSearchUtil.logPayload;

public class CountQueryFactory {

    private CountQueryFactory() {
        // only static methods
    }

    public static SearchRequest createTerminologyCountQuery(TerminologySearchRequest request,
                                                            YtiUser user,
                                                            Set<String> matchingConcepts,
                                                            Set<UUID> privilegedOrganizations) {
        var terminologyQuery = TerminologyQueryFactory.getTerminologyBaseQuery(request,
                user.isSuperuser(),
                matchingConcepts,
                privilegedOrganizations);

        var sr = new SearchRequest.Builder()
                .index(IndexService.TERMINOLOGY_INDEX)
                .size(0)
                .query(terminologyQuery)
                .aggregations("statuses", getAggregation("status"))
                .aggregations("groups", getAggregation("groups"))
                .aggregations("languages", getAggregation("languages"))
                .build();

        logPayload(sr, IndexService.TERMINOLOGY_INDEX);
        return sr;
    }

    public static SearchRequest createConceptCountQuery(ConceptSearchRequest request,
                                                        boolean isSuperUser,
                                                        Set<String> incompleteFromTerminologies) {
        var query = ConceptQueryFactory.getConceptBaseQuery(request,
                isSuperUser,
                incompleteFromTerminologies);

        var sr = new SearchRequest.Builder()
                .index(IndexService.CONCEPT_INDEX)
                .query(query)
                .aggregations("statuses", getAggregation("status"))
                .build();

        logPayload(sr, IndexService.CONCEPT_INDEX);
        return sr;
    }

    private static Aggregation getAggregation(String fieldName) {
        return AggregationBuilders.terms()
                .field(fieldName)
                .size(100)
                .build().
                _toAggregation();
    }
}
